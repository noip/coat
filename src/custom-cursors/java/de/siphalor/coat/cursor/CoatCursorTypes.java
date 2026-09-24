package de.siphalor.coat.cursor;

import com.mojang.blaze3d.platform.cursor.CursorType;
//# if MC_VERSION_NUMBER >= 260300
import com.mojang.blaze3d.platform.cursor.CursorTypes;
//# else
//- import com.mojang.blaze3d.platform.NativeImage;
//- import com.mojang.datafixers.util.Pair;
//- import org.jetbrains.annotations.Nullable;
//- import org.lwjgl.glfw.GLFW;
//- import org.lwjgl.glfw.GLFWImage;
//- import org.lwjgl.system.MemoryUtil;
//- import java.nio.ByteBuffer;
//# end
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoatCursorTypes {
//# if MC_VERSION_NUMBER >= 260300
	public static CursorType helpCursor() {
		return CursorTypes.POINTING_HAND;
	}

	public static void initialize() {
	}
//# else
//- 	private static Pair<CursorType, ByteBuffer> helpCursor;

//- 	public static CursorType helpCursor() {
//- 		return helpCursor.getFirst();
//- 	}

//- 	public static void initialize() {
//- 		helpCursor = createCursor("help");
//- 	}

//- 	private static Pair<CursorType, @Nullable ByteBuffer> createCursor(String name) {
//- 		try (var is = CoatCursorTypes.class.getClassLoader().getResourceAsStream("coat/cursor/" + name + ".png")) {
//- 			if (is == null) {
//- 				throw new IllegalStateException("Failed to load help cursor");
//- 			}

//- 			var nativeImage = NativeImage.read(is);
//- 			int[] pixels = nativeImage.getPixels();
//- 			var glfwImage = GLFWImage.create();

//- 			// Allocate native memory that GLFW can use
//- 			ByteBuffer pixelBuffer = MemoryUtil.memAlloc(pixels.length * 4);
//- 			for (int pixel : pixels) {
//- 				pixelBuffer.putInt(pixel);
//- 			}
//- 			pixelBuffer.flip();

//- 			glfwImage.set(nativeImage.getWidth(), nativeImage.getHeight(), pixelBuffer);
//- 			long helpCursorHandle = GLFW.glfwCreateCursor(glfwImage, 0, 0);

//- 			return Pair.of(new CursorType("coat:" + name, helpCursorHandle), pixelBuffer);
//- 		} catch (Exception e) {
//- 			log.error("Failed to load help cursor", e);
//- 			return Pair.of(CursorType.DEFAULT, null);
//- 		}
//- 	}
//# end
}
