package io.github.irgaly.test.platform

import kotlinx.cinterop.*
import platform.Foundation.*

actual class Files {
    actual companion object {
        actual fun createTemporaryDirectory(): String {
            return memScoped {
                // https://developer.apple.com/documentation/foundation/1409211-nstemporarydirectory
                // iOS: NSTemporaryDirectory() = (Application Sandbox)/tmp
                // macOS: NSTemporaryDirectory() = /var/folders/...
                // https://developer.apple.com/documentation/foundation/filemanager/1407693-url
                val manager = NSFileManager.defaultManager
                val errorPtr = alloc<ObjCObjectVar<NSError?>>().ptr
                val result = manager.URLForDirectory(
                    directory = NSItemReplacementDirectory,
                    inDomain = NSUserDomainMask,
                    appropriateForURL = manager.temporaryDirectory,
                    create = true,
                    error = errorPtr
                )?.path
                val error = errorPtr.pointed.value
                if (error != null) {
                    throw Exception(error.toString())
                }
                checkNotNull(result)
            }
        }

        actual fun deleteRecursively(directoryPath: String): Boolean {
            return memScoped {
                // https://developer.apple.com/documentation/foundation/nsfilemanager/1413590-removeitematurl
                val manager = NSFileManager.defaultManager
                val errorPtr = alloc<ObjCObjectVar<NSError?>>().ptr
                val removed = manager.removeItemAtURL(
                    URL = NSURL(fileURLWithPath = directoryPath),
                    error = errorPtr
                )
                val error = errorPtr.pointed.value
                if (error != null) {
                    throw Exception(error.toString())
                }
                removed
            }
        }
    }
}
