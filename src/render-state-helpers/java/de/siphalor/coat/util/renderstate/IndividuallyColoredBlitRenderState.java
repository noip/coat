package de.siphalor.coat.util.renderstate;

//# if MC_VERSION_NUMBER >= 260300
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
//# else
//- import com.mojang.blaze3d.pipeline.RenderPipeline;
//# end
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
//# if MC_VERSION_NUMBER >= 260100
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
//# else
//- import net.minecraft.client.gui.render.state.GuiElementRenderState;
//# end
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;
import org.joml.Vector4f;

public record IndividuallyColoredBlitRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2f pose,
		ScreenRectangle rect,
		Vector4f uv,
		int topLeftColor,
		int topRightColor,
		int bottomRightColor,
		int bottomLeftColor,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

	public IndividuallyColoredBlitRenderState(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2f pose,
			ScreenRectangle rect,
			Vector4f uv,
			int topLeftColor,
			int topRightColor,
			int bottomRightColor,
			int bottomLeftColor,
			@Nullable ScreenRectangle scissorArea
	) {
		this(
				pipeline,
				textureSetup,
				pose,
				rect,
				uv,
				topLeftColor,
				topRightColor,
				bottomRightColor,
				bottomLeftColor,
				scissorArea,
				RenderStateHelper.computeRectBounds(rect, pose, scissorArea)
		);
	}

	//# if MC_VERSION_NUMBER >= 12110
	@Override
	public void buildVertices(VertexConsumer vertexConsumer) {
		vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.top()).setUv(uv.x, uv.y).setColor(topLeftColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.bottom()).setUv(uv.x, uv.w).setColor(bottomLeftColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.bottom()).setUv(uv.z, uv.w).setColor(bottomRightColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.top()).setUv(uv.z, uv.y).setColor(topRightColor);
	}
	//# else
	//- @Override
	//- public void buildVertices(VertexConsumer vertexConsumer, float z) {
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.top(), z).setUv(uv.x, uv.y).setColor(topLeftColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.bottom(), z).setUv(uv.x, uv.w).setColor(bottomLeftColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.bottom(), z).setUv(uv.z, uv.w).setColor(bottomRightColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.top(), z).setUv(uv.z, uv.y).setColor(topRightColor);
	//- }
	//# end
}
