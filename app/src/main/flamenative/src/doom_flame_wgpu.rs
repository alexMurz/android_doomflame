use std::mem::size_of;

use rand::Rng;
use wgpu::{util::DeviceExt, BufferUsages};

const LOCAL_GROUP_SIZE: u32 = 16;

pub struct DoomFlame {
    // gpu data
    dev: wgpu::Device,
    queue: wgpu::Queue,
    pipeline: wgpu::ComputePipeline,
    color_buffer: wgpu::Buffer,
    bind_group1: wgpu::BindGroup,
    bind_group2: wgpu::BindGroup,
    meta_bind_group: wgpu::BindGroup,
    // metadata
    swap: bool,
    resolution: usize,
    tracking_pixmap: Vec<u32>,
}

impl crate::DoomFlameRunner for DoomFlame {
    fn create(resolution: usize, palette: Vec<u32>) -> Self {
        let temp_buffer_size = resolution * resolution * size_of::<u32>();

        let (dev, queue) = futures::executor::block_on(create_wgpu());

        ////////////////////////////////////////////////////////////////////////////////////
        // Meta-data resources
        ////////////////////////////////////////////////////////////////////////////////////
        let meta_buffer = {
            let mut data = palette.clone();
            data.insert(0, resolution as u32);

            let desc = wgpu::util::BufferInitDescriptor {
                label: Some("metadata-buffer"),
                usage: BufferUsages::STORAGE,
                contents: bytemuck::cast_slice(&data),
            };
            dev.create_buffer_init(&desc)
        };

        // Seeded randomness for each compute unit
        let noise_buffer = {
            let mut r = rand::thread_rng();
            let mut noise = Vec::<f32>::with_capacity(resolution * resolution);
            for _ in 0..resolution * resolution {
                noise.push(r.gen());
            }
            let desc = wgpu::util::BufferInitDescriptor {
                label: Some("noise-buffer"),
                usage: BufferUsages::STORAGE,
                contents: bytemuck::cast_slice(&noise),
            };

            dev.create_buffer_init(&desc)
        };

        ////////////////////////////////////////////////////////////////////////////////////
        // Create resources
        ////////////////////////////////////////////////////////////////////////////////////

        let mut initial_temp_buffer = vec![0u32; resolution * resolution];
        // Seed last line with max temperature
        {
            let max = resolution * resolution;
            for i in max - resolution..max {
                initial_temp_buffer[i] = palette.len() as u32 - 1;
            }
        }

        let temp_buffer_desc = wgpu::util::BufferInitDescriptor {
            label: None,
            usage: wgpu::BufferUsages::STORAGE,
            contents: bytemuck::cast_slice(&initial_temp_buffer),
        };

        let buffer1 = dev.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("temperature-buffer-1"),
            ..temp_buffer_desc
        });
        let buffer2 = dev.create_buffer_init(&wgpu::util::BufferInitDescriptor {
            label: Some("temperature-buffer-2"),
            ..temp_buffer_desc
        });

        let color_buffer = dev.create_buffer(&wgpu::BufferDescriptor {
            label: Some("color-buffer"),
            size: temp_buffer_size as u64,
            usage: wgpu::BufferUsages::STORAGE | wgpu::BufferUsages::MAP_READ,
            mapped_at_creation: false,
        });

        let tracking_pixmap = vec![palette[0]; resolution * resolution];

        ////////////////////////////////////////////////////////////////////////////////////
        // Create compute pipeline
        ////////////////////////////////////////////////////////////////////////////////////

        let spirv_source = wgpu::include_spirv!("../shader/flame.comp.spv");

        let cs_module = dev.create_shader_module(spirv_source);

        let pipeline = dev.create_compute_pipeline(&wgpu::ComputePipelineDescriptor {
            label: Some("doomflame-pipeline"),
            layout: None,
            module: &cs_module,
            entry_point: "main",
        });

        let buffer_bind_group_layout = pipeline.get_bind_group_layout(0);
        let meta_bind_group_layout = pipeline.get_bind_group_layout(1);

        ////////////////////////////////////////////////////////////////////////////////////
        // Prebuild bind groups
        ////////////////////////////////////////////////////////////////////////////////////

        let bind_group1 = dev.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("temperature-binding-1"),
            layout: &buffer_bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: buffer1.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: buffer2.as_entire_binding(),
                },
            ],
        });

        let bind_group2 = dev.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("temperature-binding-2"),
            layout: &buffer_bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: buffer2.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: buffer1.as_entire_binding(),
                },
            ],
        });

        let meta_bind_group = dev.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("fixed-data-bindgroup"),
            layout: &meta_bind_group_layout,
            entries: &[
                wgpu::BindGroupEntry {
                    binding: 0,
                    resource: color_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 1,
                    resource: meta_buffer.as_entire_binding(),
                },
                wgpu::BindGroupEntry {
                    binding: 2,
                    resource: noise_buffer.as_entire_binding(),
                },
            ],
        });

        Self {
            dev,
            queue,
            pipeline,
            color_buffer,
            bind_group1,
            bind_group2,
            meta_bind_group,
            swap: false,
            resolution,
            tracking_pixmap,
        }
    }

    fn update(&mut self) {
        let Self {
            dev,
            queue,
            ref pipeline,
            ref color_buffer,
            ref bind_group1,
            ref bind_group2,
            ref meta_bind_group,
            resolution,
            swap,
            tracking_pixmap,
            ..
        } = self;

        let resolution = *resolution as u32;

        let bind_group = if *swap { bind_group1 } else { bind_group2 };

        // Enqueue work with bind_group
        let mut encoder = dev.create_command_encoder(&wgpu::CommandEncoderDescriptor::default());
        {
            let mut pass = encoder.begin_compute_pass(&wgpu::ComputePassDescriptor::default());
            pass.set_pipeline(pipeline);
            pass.set_bind_group(0, bind_group, &[]);
            pass.set_bind_group(1, meta_bind_group, &[]);

            let group_size = resolution / LOCAL_GROUP_SIZE;
            pass.dispatch_workgroups(group_size, group_size, 1);
        }

        let cb = encoder.finish();

        queue.submit(Some(cb));

        // Copy from color buffer to tracking_pixmap
        {
            let buffer_slice = color_buffer.slice(..);

            let (send, recv) = futures::channel::oneshot::channel();
            buffer_slice.map_async(wgpu::MapMode::Read, |x| {
                send.send(x).ok();
            });

            // Wait for idle
            dev.poll(wgpu::MaintainBase::Wait);
            // Wait for mapping result
            futures::executor::block_on(recv).unwrap().unwrap();

            let buffer_view = buffer_slice.get_mapped_range();
            let result = bytemuck::cast_slice(&*buffer_view);
            tracking_pixmap.copy_from_slice(result);

            // drop bufferview before unmapping memory
            drop(buffer_view);
            color_buffer.unmap();
        }

        // swap buffers
        *swap = !*swap;
    }

    fn get_tracking_pixmap(&self) -> &[u32] {
        &self.tracking_pixmap
    }
}

async fn create_wgpu() -> (wgpu::Device, wgpu::Queue) {
    let instance = wgpu::Instance::new(wgpu::Backends::PRIMARY);
    let adapter = instance
        .request_adapter(&wgpu::RequestAdapterOptions::default())
        .await
        .unwrap();

    adapter
        .request_device(
            &wgpu::DeviceDescriptor {
                label: None,
                features: wgpu::Features::empty(),
                limits: wgpu::Limits::downlevel_defaults(),
            },
            None,
        )
        .await
        .unwrap()
}

#[test]
fn check_runs() {
    use crate::DoomFlameRunner;
    let mut f = DoomFlame::create(10, crate::FIRE_PALETTE.to_vec());
    f.update();
}
