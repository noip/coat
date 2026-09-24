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

public record IndividuallyColoredRectangleRenderState(
		RenderPipeline pipeline,
		TextureSetup textureSetup,
		Matrix3x2f pose,
		ScreenRectangle rect,
		int topLeftColor,
		int topRightColor,
		int bottomRightColor,
		int bottomLeftColor,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
		) implements GuiElementRenderState {

	public IndividuallyColoredRectangleRenderState(
			RenderPipeline pipeline,
			TextureSetup textureSetup,
			Matrix3x2f pose,
			ScreenRectangle rect,
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
		vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.top()).setColor(topLeftColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.bottom()).setColor(bottomLeftColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.bottom()).setColor(bottomRightColor);
		vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.top()).setColor(topRightColor);
	}
	//# else
	//- @Override
	//- public void buildVertices(VertexConsumer vertexConsumer, float z) {
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.top(), z).setColor(topLeftColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.left(), rect.bottom(), z).setColor(bottomLeftColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.bottom(), z).setColor(bottomRightColor);
	//- 	vertexConsumer.addVertexWith2DPose(pose, rect.right(), rect.top(), z).setColor(topRightColor);
	//- }
	//# end
}
