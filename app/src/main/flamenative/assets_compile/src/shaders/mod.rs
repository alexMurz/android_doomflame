use std::{
    collections::HashMap,
    fs,
    io::Read,
    path::{Path, PathBuf},
};

use crate::FileRef;

use self::types::ShaderFileType;

mod types;

trait UnwrapDisplay<T> {
    fn unwrap_display(self) -> T;
}

impl<T, E: std::fmt::Display> UnwrapDisplay<T> for std::result::Result<T, E> {
    fn unwrap_display(self) -> T {
        match self {
            Ok(v) => v,
            Err(e) => panic!("{}", e.to_string()),
        }
    }
}

enum Change {
    Create(CreateChange),
    Update(UpdateChange),
    Delete(DeleteChange),
}

struct CreateChange {
    pub src: FileRef,
    pub dst: PathBuf,
    pub ty: types::ShaderFileType,
}

struct UpdateChange {
    pub src: FileRef,
    pub dst: FileRef,
    pub ty: types::ShaderFileType,
}

struct DeleteChange {
    pub dst: FileRef,
}

pub fn compile_shaders() {
    println!("BEGIN - compile_shaders");
    let changes = collect_changes();

    println!("{} changes found", changes.len());

    for c in changes.into_iter() {
        match c {
            Change::Create(change) => {
                println!(
                    "Create: {:?} -> {:?} :: {:?}",
                    change.src.path, change.dst, change.ty
                );
                compile_and_save(change.src.path, change.dst, change.ty).unwrap_display();
            }
            Change::Update(change) => {
                println!(
                    "Update: {:?} -> {:?} :: {:?}",
                    change.src.path, change.dst.path, change.ty
                );
                compile_and_save(change.src.path, change.dst.path, change.ty).unwrap_display();
            }
            Change::Delete(change) => {
                println!("Delete: {}", change.dst.name);
                fs::remove_file(change.dst.path).unwrap_display();
            }
        }
    }

    println!("END - compile_shaders");
}

fn compile_and_save(
    src: impl AsRef<Path>,
    dst: impl AsRef<Path>,
    ty: types::ShaderFileType,
) -> Result<(), String> {
    let code =
        fs::read_to_string(src).map_err(|err| format!("Read code error: {}", err.to_string()))?;

    let mut src = glsl_to_spirv::compile(code.as_str(), ty.glsl_type())?;

    let mut spir_code = Vec::new();

    src.read_to_end(&mut spir_code)
        .map_err(|err| format!("Read spid-code error: {}", err.to_string()))?;

    fs::write(dst, spir_code)
        .map_err(|err| format!("Write spir output error: {}", err.to_string()))?;

    Ok(())
}

fn collect_changes() -> Vec<Change> {
    let inputs = collect_dir(crate::SHADER_SRC);
    fs::create_dir_all(crate::SHADER_DST).ok();
    let mut outputs = collect_dir(crate::SHADER_DST);

    let mut changes = Vec::new();

    for (k, from) in inputs.into_iter() {
        let ty = match ShaderFileType::from_file_extension(&from.ext) {
            Some(v) => v,
            None => continue,
        };
        let to = outputs.remove(&k);

        if let Some(to) = to {
            if from > to {
                // Target is older then source
                changes.push(Change::Update(UpdateChange {
                    src: from,
                    dst: to,
                    ty,
                }));
            } else {
                // Do nothing, file not update
            }
        } else {
            // Target does not exist
            let mut dst = PathBuf::new();
            dst.push(crate::SHADER_DST);
            dst.push(format!("{}.{}.spv", from.name, ty.extension()));

            changes.push(Change::Create(CreateChange { src: from, dst, ty }))
        }
    }

    for (_, dst) in outputs.into_iter() {
        changes.push(Change::Delete(DeleteChange { dst }));
    }

    changes
}

fn collect_dir(dir_path: &str) -> HashMap<String, FileRef> {
    let dir = fs::read_dir(dir_path).unwrap_display();

    let mut result = HashMap::new();
    for v in dir {
        match v {
            Ok(entry) => {
                let file = FileRef::new(entry.path()).expect("Create FileRef from");

                // Convert `frag.spv` -> `frag`
                let ext_split = file.ext.find('.');
                let ext = if let Some(split) = ext_split {
                    &file.ext[..split]
                } else {
                    &file.ext
                };

                let name = format!("{}.{}", file.name, ext);
                result.insert(name, file);
            }
            Err(e) => {
                eprintln!("Read input files error {:?}", e);
            }
        }
    }

    result
}
