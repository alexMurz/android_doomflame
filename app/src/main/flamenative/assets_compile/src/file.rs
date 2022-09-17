use std::{
    fs::{self, File, OpenOptions},
    io,
    path::{Path, PathBuf},
    time::SystemTime,
};

#[derive(Debug)]
pub struct FileRef {
    pub path: PathBuf,
    pub name: String,
    pub ext: String,
    pub updated: SystemTime,
}

impl PartialEq for FileRef {
    fn eq(&self, other: &Self) -> bool {
        self.path == other.path
            && self.name == other.name
            && self.ext == other.ext
            && self.updated == other.updated
    }
}

impl PartialOrd for FileRef {
    #[inline]
    fn partial_cmp(&self, other: &Self) -> Option<std::cmp::Ordering> {
        self.updated.partial_cmp(&other.updated)
    }
}

impl FileRef {
    pub fn new<FilePath>(path: FilePath) -> io::Result<Self>
    where
        FilePath: AsRef<Path>,
    {
        let meta = fs::metadata(&path)?;
        let updated = meta.modified()?;
        let path = path.as_ref().to_path_buf();

        let full_name = path.file_name().and_then(|x| x.to_str()).expect("filename");
        let split_at = full_name.find('.').expect("Full name . symbol");
        let name = full_name[..split_at].to_string();
        let ext = full_name[split_at + 1..].to_string();

        Ok(Self {
            path,
            updated,
            name,
            ext,
        })
    }

    #[inline]
    pub fn open(&self, options: &OpenOptions) -> io::Result<File> {
        options.open(self.path.as_path())
    }
}
