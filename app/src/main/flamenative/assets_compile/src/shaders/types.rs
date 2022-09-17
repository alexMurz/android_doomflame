#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum ShaderFileType {
    Vertex,
    Fragment,
    Compute,
}

impl ShaderFileType {
    pub fn from_file_extension(ext: &str) -> Option<Self> {
        let ext = if ext.starts_with(".") { &ext[1..] } else { ext };
        let ty = match ext {
            "vert" | "vs" => Self::Vertex,
            "frag" | "fs" => Self::Fragment,
            "comp" => Self::Compute,
            _ => return None,
        };
        Some(ty)
    }

    pub fn extension(self) -> &'static str {
        match self {
            Self::Vertex => "vert",
            Self::Fragment => "frag",
            Self::Compute => "comp",
        }
    }

    pub fn glsl_type(self) -> glsl_to_spirv::ShaderType {
        match self {
            Self::Vertex => glsl_to_spirv::ShaderType::Vertex,
            Self::Fragment => glsl_to_spirv::ShaderType::Fragment,
            Self::Compute => glsl_to_spirv::ShaderType::Compute,
        }
    }
}
