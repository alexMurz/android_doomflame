use rand::Rng;

pub struct DoomFlame {
    resolution: usize,
    buffer: Vec<i32>,
    tracking_pixmap: Vec<u32>,
    palette: Vec<u32>,
}

impl crate::DoomFlameRunner for DoomFlame {
    fn create(resolution: usize, palette: Vec<u32>) -> Self {
        let max_temp = palette.len() as i32 - 1;
        let mut buffer = vec![0; resolution * resolution];

        let r = resolution as i32;
        for i in as_idx(0, r - 1, r)..as_idx(r, r - 1, r) {
            buffer[i] = max_temp;
        }

        Self {
            resolution,
            buffer,
            tracking_pixmap: vec![palette[0]; resolution * resolution],
            palette,
        }
    }

    fn update(&mut self) {
        let mut rng = rand::thread_rng();
        let s = self.resolution as i32;
        let buffer = &mut self.buffer;

        for x in 0..s {
            for y in 0..s - 1 {
                let dx: i32 = rng.gen_range(-1..=1);
                let dy: i32 = rng.gen_range(0..=5);
                let dt: i32 = rng.gen_range(0..=2);

                let x1 = (x as i32 + dx).clamp(0, s - 1);
                let y1 = (y as i32 + dy).clamp(0, s - 1);

                let this_idx = as_idx(x, y, s);
                let temp = (buffer[as_idx(x1, y1, s)] - dt).max(0);
                buffer[this_idx] = temp;

                self.tracking_pixmap[this_idx] = self.palette[temp as usize];
            }
        }
    }

    fn get_tracking_pixmap(&self) -> &[u32] {
        &self.tracking_pixmap
    }
}

#[inline]
fn as_idx(x: i32, y: i32, w: i32) -> usize {
    (x + y * w) as usize
}
