mod doom_flame;
mod doom_flame_wgpu;

// ABGR
pub static FIRE_PALETTE: &[u32] = &[
    0xFF070707, 0xFF07071F, 0xFF070F2F, 0xFF070F47, 0xFF071757, 0xFF071F67, 0xFF071F77, 0xFF07278F,
    0xFF072F9F, 0xFF073FAF, 0xFF0747BF, 0xFF0747C7, 0xFF074FDF, 0xFF0757DF, 0xFF0757DF, 0xFF075FD7,
    0xFF075FD7, 0xFF0F67D7, 0xFF0F6FCF, 0xFF0F77CF, 0xFF0F7FCF, 0xFF1787CF, 0xFF1787C7, 0xFF178FC7,
    0xFF1F97C7, 0xFF1F9FBF, 0xFF1F9FBF, 0xFF27A7BF, 0xFF27A7BF, 0xFF2FAFBF, 0xFF2FAFB7, 0xFF2FB7B7,
    0xFF37B7B7, 0xFF6FCFCF, 0xFF9FDFDF, 0xFFC7EFEF, 0xFFFFFFFF,
];

pub trait DoomFlameRunner {
    fn create(resolution: usize, palette: Vec<u32>) -> Self
    where
        Self: Sized;

    fn update(&mut self);

    fn get_tracking_pixmap(&self) -> &[u32];
}

#[cfg(target_os = "android")]
mod jni_main {
    use super::*;

    use jni::{
        objects::JClass,
        sys::{jint, jlong, jobject},
        JNIEnv,
    };
    use ndk::bitmap::AndroidBitmap;

    type Inst = doom_flame_wgpu::DoomFlame;

    fn to_ptr(value: Inst) -> jlong {
        let ptr = Box::into_raw(Box::new(value));
        ptr as jlong
    }

    unsafe fn from_ptr<'a>(ptr: jlong) -> &'a mut Inst {
        let ptr = ptr as *mut Inst;
        &mut *ptr
    }

    unsafe fn drop_ptr(ptr: jlong) {
        let ptr = ptr as *mut Inst;
        std::ptr::drop_in_place(ptr)
    }

    #[no_mangle]
    pub unsafe extern "system" fn Java_com_example_doomflame_doom_1flame_DoomFlameNDKBindings_create(
        _: JNIEnv,
        _: JClass,
        resolution: jint
    ) -> jlong {
        let inst = Inst::create(resolution as usize, FIRE_PALETTE.to_vec());
        return to_ptr(inst);
    }

    #[no_mangle]
    pub unsafe extern "system" fn Java_com_example_doomflame_doom_1flame_DoomFlameNDKBindings_update(
        env: JNIEnv,
        _: JClass,
        ptr: jlong,
        dst: jobject,
    ) {
        let flame = from_ptr(ptr);
        flame.update();

        let bitmap = AndroidBitmap::from_jni(env.get_native_interface(), dst);
        let info = bitmap.get_info().unwrap();

        let target = bitmap.lock_pixels().unwrap() as *mut u32;
        let as_slice =
            std::slice::from_raw_parts_mut(target, (info.width() * info.height()) as usize);

        as_slice.copy_from_slice(flame.get_tracking_pixmap());

        bitmap.unlock_pixels().unwrap();
    }

    #[no_mangle]
    pub unsafe extern "system" fn Java_com_example_doomflame_doom_1flame_DoomFlameNDKBindings_destroy(
        _: JNIEnv,
        _: JClass,
        ptr: jlong,
    ) {
        drop_ptr(ptr);
    }
}
