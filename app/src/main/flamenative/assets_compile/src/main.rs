mod shaders;

mod file;

pub use file::*;

pub const SHADER_SRC: &'static str = "../shader_sources";
pub const SHADER_DST: &'static str = "../shader";

fn main() {
    shaders::compile_shaders();
}
