package de.siphalor.coat.list;

import com.google.common.collect.Lists;
//- import com.mojang.blaze3d.systems.RenderSystem;
//- import com.mojang.blaze3d.vertex.BufferBuilder;
//- import com.mojang.blaze3d.vertex.BufferUploader;
//- import com.mojang.blaze3d.vertex.DefaultVertexFormat;
//- import com.mojang.blaze3d.vertex.PoseStack;
//- import com.mojang.blaze3d.vertex.Tesselator;
//- import com.mojang.blaze3d.vertex.VertexFormat;
//- import de.siphalor.coat.util.CoatColor;
//- import de.siphalor.coat.util.CoatColor;
//- import com.mojang.blaze3d.systems.RenderSystem;
import de.siphalor.coat.util.CoatUtil;
import de.siphalor.coat.util.TickableElement;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import lombok.Getter;
import lombok.Setter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
//- import net.minecraft.client.gui.GuiGraphics;
//- import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Renderable;
//- import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
//- import net.minecraft.client.renderer.GameRenderer;
//- import net.minecraft.client.renderer.ShaderManager;
//- import net.minecraft.client.renderer.ShaderProgram;
//- import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
//- import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//- import org.lwjgl.opengl.GL11;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This is a reimplementation of {@link net.minecraft.client.gui.components.AbstractSelectionList} to enable variable item heights.
 */
@Environment(EnvType.CLIENT)
public class DynamicEntryListWidget<E extends DynamicEntryListWidget.Entry> extends AbstractContainerEventHandler
		implements EntryContainer, TickableElement,
		/*# if MC_VERSION_NUMBER >= 11700 */NarratableEntry,/*# end */
		/*# if MC_VERSION_NUMBER >= 11903 */Renderable/*# else *//*- Widget *//*# end */
{
	private static final int TOP_PADDING = 8;
	private static final int BOTTOM_PADDING = 6;
	private static final int SCROLLBAR_WIDTH = 6;
	//# if MC_VERSION_NUMBER >= 12111
	private static final Identifier SCROLLBAR_BACKGROUND_TEXTURE = AbstractSelectionList.SCROLLER_BACKGROUND_SPRITE;
	//# elif MC_VERSION_NUMBER >= 12005
	//- private static final ResourceLocation SCROLLBAR_BACKGROUND_TEXTURE = AbstractSelectionList.SCROLLER_BACKGROUND_SPRITE;
	//# else
	//- private static final CoatColor SCROLLBAR_BACKGROUND_COLOR = CoatColor.rgb(0x000000);
	//# end
	//# if MC_VERSION_NUMBER >= 12111
	private static final Identifier SCROLLBAR_HANDLE_TEXTURE = AbstractSelectionList.SCROLLER_SPRITE;
	//# elif MC_VERSION_NUMBER >= 12004
	//- private static final ResourceLocation SCROLLBAR_HANDLE_TEXTURE = AbstractSelectionList.SCROLLER_SPRITE;
	//# else
	//- private static final CoatColor SCROLLBAR_HANDLE_SHADOW_COLOR = CoatColor.rgb(0x808080);
	//- private static final CoatColor SCROLLBAR_HANDLE_COLOR = CoatColor.rgb(0xC0C0C0);
	//# end

	protected final Minecraft minecraft;
	private final Entries entries = new Entries();
	protected int width;
	protected int height;
	protected int top;
	protected int bottom;
	protected int right;
	protected int left;
	private int rowWidth;
	/**
	 * The current scroll position - aka the vertical offset.
	 */
	@Getter
	private double scrollYOffset;
	//# if !TRANSPARENT_MENUS
	//- /**
	//-  * The brightness with which to render the background.
	//-  * The value must be between 0 (completely black) and 1 (normal image).
	//-  * The default value is <code>0.27F</code>
	//-  */
	//- @Setter
	//- private float backgroundBrightness = 0.27F;
	//# end
	/**
	 * The identifier for the background associated with this widget
	 */
	@Getter
	@Setter
	//# if MC_VERSION_NUMBER >= 12111
	private Identifier background =
	//# else
	//- private ResourceLocation background =
	//# end
			//# if MC_VERSION_NUMBER >= 12111
			Identifier.parse("textures/block/dark_oak_planks.png");
			//# elif MC_VERSION_NUMBER >= 12109
			//- ResourceLocation.parse("textures/block/dark_oak_planks.png");
			//# elif MC_VERSION_NUMBER >= 12100
			//- ResourceLocation.parse("textures/block/tuff_bricks.png");
			//# elif MC_VERSION_NUMBER >= 12000
			//- new ResourceLocation("textures/block/cherry_log.png");
			//# elif MC_VERSION_NUMBER >= 11900
			//- new ResourceLocation("textures/block/mangrove_log.png");
			//# elif MC_VERSION_NUMBER >= 11800
			//- new ResourceLocation("textures/block/dripstone_block.png");
			//# elif MC_VERSION_NUMBER >= 11700
			//- new ResourceLocation("textures/block/smooth_basalt.png");
			//# elif MC_VERSION_NUMBER >= 11600
			//- new ResourceLocation("textures/block/blackstone_top.png");
			//# end
	private boolean scrolling;

	/**
	 * Constructs a new instance. You can ignore this constructor safely under most circumstances.
	 *
	 * @param minecraft   The {@link Minecraft} instance
	 * @param width    The width to take up
	 * @param height   The height to take up
	 * @param top      The top position
	 * @param rowWidth The maximum width of the contained entries
	 */
	public DynamicEntryListWidget(Minecraft minecraft, int width, int height, int top, int rowWidth) {
		this.minecraft = minecraft;
		this.width = width;
		this.height = height;
		this.top = top;
		this.bottom = top + height;
		this.left = 0;
		this.right = width;
		this.rowWidth = rowWidth;
	}

	/**
	 * Constructs a new instance. Typically used for config screens.
	 *
	 * @param minecraft     The {@link Minecraft} instance
	 * @param entries    A collection of entries to be immediately added to this widget
	 * @param background An identifier referring to a background texture for this widget
	 */
	public DynamicEntryListWidget(
			Minecraft minecraft,
			Collection<E> entries,
			//# if MC_VERSION_NUMBER >= 12111
			@Nullable Identifier background
			//# else
			//- @Nullable ResourceLocation background
			//# end
	) {
		this.minecraft = minecraft;
		top = 20;
		addEntries(entries);
		if (background != null) {
			this.background = background;
		}
	}

	/**
	 * Gets the padding that's used on the left and right side of the entries.
	 *
	 * @return The padding for one side
	 */
	public int getHorizontalPadding() {
		return 4;
	}

	/**
	 * Gets actual width of the entries.
	 *
	 * @return The entry width
	 */
	public int getEntryWidth() {
		return Math.min(rowWidth, width) - getHorizontalPadding() * 2;
	}

	/**
	 * {@inheritDoc}
	 */
	public List<E> entries() {
		return entries;
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return entries;
	}

	/**
	 * Gets an entry by its index.
	 *
	 * @param index The index to lookup the entry by
	 * @return The index at the specified index or <code>null</code> if no such entry exists
	 */
	public E getEntry(int index) {
		return entries.get(index);
	}

	/**
	 * Adds an entry to the end of this widget.
	 *
	 * @param entry A new entry
	 */
	public void addEntry(E entry) {
		entry.setParent(this);
		entries.add(entry);
	}

	/**
	 * Inserts an entry at the specified position in this widget.
	 *
	 * @param position The position to target
	 * @param entry    The entry to add
	 */
	public void addEntry(int position, E entry) {
		entry.setParent(this);
		entries.add(position, entry);
	}

	/**
	 * Adds a collection of entries at once to the end of this widget
	 *
	 * @param newEntries New entries to add
	 */
	public void addEntries(Collection<E> newEntries) {
		for (E newEntry : newEntries) {
			newEntry.setParent(this);
		}
		entries.addAll(newEntries);
	}

	/**
	 * Gets the current amount of entries in this widget.
	 *
	 * @return The count of entries
	 */
	protected int getEntryCount() {
		return this.entries().size();
	}

	/**
	 * Tries to find an entry at the given screen position.
	 *
	 * @param x The x position to target
	 * @param y The y position to target
	 * @return The entry at that position or <code>null</code> if there is no entry at that position
	 */
	@Nullable
	protected final E getEntryAtPosition(double x, double y) {
		int halfRowWidth = this.getEntryWidth() / 2;
		int screenCenter = this.left + this.width / 2;
		int rowLeft = screenCenter - halfRowWidth;
		int rowRight = screenCenter + halfRowWidth;
		if (x >= getScrollbarPositionX() || x < rowLeft || x > rowRight) {
			return null;
		}
		y -= getEntryAreaTop();
		if (y < 0 || y > getMaxEntryPosition()) {
			return null;
		}
		IntListIterator iterator = entries.bottoms.iterator();
		while (iterator.hasNext()) {
			if (y < iterator.nextInt()) {
				return getEntry(iterator.nextIndex() - 1);
			}
		}
		return null;
	}

	/**
	 * Finds the matching entry index for the given y coordinate
	 *
	 * @param y The y coordinate to look for
	 * @return The nearest entry index
	 */
	protected int getEntryAtY(int y) {
		y -= getEntryAreaTop();
		if (y < 0) {
			return 0;
		}
		if (y > getMaxEntryPosition()) {
			return getEntryCount();
		}
		IntListIterator iterator = entries.bottoms.iterator();
		while (iterator.hasNext()) {
			int cur = iterator.nextInt();
			if (y < cur) {
				return iterator.previousIndex();
			}
		}
		return getEntryCount() - 1;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void entryHeightChanged(GuiEventListener element) {
		//noinspection SuspiciousMethodCalls
		int index = entries.indexOf(element);
		int bottom = index == 0 ? 0 : entries.bottoms.getInt(index - 1);
		for (int i = index, l = entries.size(); i < l; i++) {
			bottom += entries.get(i).getHeight();
			entries.bottoms.set(i, bottom);
		}
	}

	/**
	 * Should be called to resize this widget.
	 *
	 * @param newWidth  The new width
	 * @param newHeight The new height
	 */
	public void resize(int newWidth, int newHeight) {
		height = newHeight;
		bottom = top + height;
		widthChanged(newWidth);
	}

	/**
	 * Should be called to resize this widget vertically.
	 *
	 * @param newWidth The new width
	 */
	public void widthChanged(int newWidth) {
		width = newWidth;
		right = left + newWidth;

		for (Entry entry : entries) {
			entry.widthChanged(getEntryWidth());
		}
	}

	/**
	 * Repositions this widget to the given position.
	 *
	 * @param left The new x position
	 * @param top  The new y position
	 */
	public void setPosition(int left, int top) {
		this.left = left;
		this.top = top;
		this.right = left + width;
		this.bottom = top + height;
	}

	/**
	 * Sets the new maximum entry width.
	 *
	 * @param rowWidth The new width
	 */
	public void setRowWidth(int rowWidth) {
		this.rowWidth = rowWidth;
		widthChanged(width);
	}

	/**
	 * Gets the combined height of all entries.
	 *
	 * @return The height of all entries combined
	 */
	protected int getMaxEntryPosition() {
		if (entries.isEmpty()) {
			return 0;
		}
		return entries.bottoms.getInt(entries.bottoms.size() - 1);
	}

	/**
	 * Gets the maximum position that can be scrolled to.
	 *
	 * @return The absolute bottom of the scroll space
	 */
	protected int getMaxPosition() {
		return getMaxEntryPosition() + BOTTOM_PADDING;
	}

	@Override
	//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	//# elif RENDERING == "GUI_GRAPHICS"
	//- public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	//# else
	//- public void render(PoseStack graphics, int mouseX, int mouseY, float delta) {
	//# end
		renderWidget(graphics, mouseX, mouseY, delta);
	}

	/**
	 * {@inheritDoc}
	 */
	//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
	public void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	//# elif RENDERING == "GUI_GRAPHICS"
	//- public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	//# else
	//- public void renderWidget(PoseStack graphics, int mouseX, int mouseY, float delta) {
	//# end

		//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR" || RENDERING == "GUI_GRAPHICS"
		renderBackground(graphics);
		renderScrollbar(graphics);
		//# else
		//- renderBackground();
		//- renderScrollbar();
		//# end

		this.renderList(graphics, mouseX, mouseY, delta);

		// render top shadow
		//# if RENDERING == "GUI_GRAPHICS"
		//- graphics.fillGradient(left, top, right, top + TOP_PADDING, 0x77000000, 0x00000000);
		//# elif RENDERING == "POSE_STACK"
		//- fillGradient(graphics, left, top, right, top + TOP_PADDING, 0x77000000, 0x00000000);
		//# end
	}

	/**
	 * Renders the background of this widget.
	 */
	//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
	protected void renderBackground(GuiGraphicsExtractor graphics) {
	//# elif RENDERING == "GUI_GRAPHICS"
	//- protected void renderBackground(GuiGraphics graphics) {
	//# else
	//- protected void renderBackground() {
	//# end
		//# if !TRANSPARENT_MENUS
		//- int colorPart = (int) (backgroundBrightness * 255F);
		//- CoatColor backgroundTint = CoatColor.rgb(colorPart, colorPart, colorPart);
		//# end

		//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR" || RENDERING == "GUI_GRAPHICS"
		//# if MC_VERSION_NUMBER < 12108
		//- RenderSystem.enableBlend();
		//# end
		//# if TRANSPARENT_MENUS
		CoatUtil.drawTiledTexture(
				graphics,
				getListBackground(),
				left,
				top,
				right,
				bottom,
				32,
				(int) getScrollYOffset()
		);
		//# else
		//- CoatUtil.drawTintedTiledTexture(
		//- 		graphics,
		//- 		getListBackground(),
		//- 		left,
		//- 		top,
		//- 		right,
		//- 		bottom,
		//- 		32,
		//- 		(int) getScrollYOffset(),
		//- 		backgroundTint
		//- );
		//# end
		//# if MC_VERSION_NUMBER < 12108
		//- RenderSystem.disableBlend();
		//# end
		//# else
		//- RenderSystem.enableDepthTest();
		//- RenderSystem.depthFunc(GL11.GL_LEQUAL);

		//- CoatUtil.drawTintedTiledTexture(
				//- getListBackground(),
				//- left,
				//- top,
				//- right,
				//- bottom,
				//- -100,
				//- 32F,
				//- (int) getScrollYOffset(),
				//- backgroundTint
		//- );
		//# end
	}

	//# if MC_VERSION_NUMBER >= 12111
	protected Identifier getListBackground() {
	//# else
	//- protected ResourceLocation getListBackground() {
	//# end
		//# if TRANSPARENT_MENUS
		return this.minecraft.level == null
				? AbstractSelectionList.MENU_LIST_BACKGROUND
				: AbstractSelectionList.INWORLD_MENU_LIST_BACKGROUND;
		//# else
		//- return background;
		//# end
	}

	//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
	private void renderScrollbar(GuiGraphicsExtractor graphics) {
	//# elif RENDERING == "GUI_GRAPHICS"
	//- private void renderScrollbar(GuiGraphics graphics) {
	//# else
	//- private void renderScrollbar() {
	//# end

		int maxScroll = this.getMaxScroll();
		if (maxScroll <= 0) {
			return;
		}

		int scrollbarLeft = this.getScrollbarPositionX();
		int scrollbarRight = scrollbarLeft + SCROLLBAR_WIDTH;

		int handleHeight = (int) ((float) (this.height * this.height) / (float) this.getMaxPosition());
		handleHeight = Mth.clamp(handleHeight, 32, this.height - 8);
		int scrollableRange = this.height - handleHeight;
		int handleTop = (int) this.getScrollYOffset() * scrollableRange / maxScroll + this.top;
		if (handleTop < this.top) {
			handleTop = this.top;
		}

		//# if MC_VERSION_NUMBER >= 12004
		//# if MC_VERSION_NUMBER >= 12005
		CoatUtil.drawTexture(
				graphics,
				SCROLLBAR_BACKGROUND_TEXTURE,
				scrollbarLeft,
				this.top,
				SCROLLBAR_WIDTH,
				this.bottom
		);
		//# else
		//- graphics.fill(scrollbarLeft, this.top, scrollbarRight, this.bottom, SCROLLBAR_BACKGROUND_COLOR.getArgb());
		//# end
		CoatUtil.drawTexture(graphics, SCROLLBAR_HANDLE_TEXTURE, scrollbarLeft, handleTop, SCROLLBAR_WIDTH, handleHeight);

		//# elif RENDERING == "GUI_GRAPHICS"
		//- graphics.fill(scrollbarLeft, top, scrollbarRight, bottom, SCROLLBAR_BACKGROUND_COLOR.getArgb());
		//- graphics.fill(scrollbarLeft, handleTop, scrollbarRight, handleTop + handleHeight, SCROLLBAR_HANDLE_SHADOW_COLOR.getArgb());
		//- graphics.fill(scrollbarLeft, handleTop, scrollbarRight - 1, handleTop + handleHeight - 1, SCROLLBAR_HANDLE_COLOR.getArgb());
		//# else
		//- //# if MC_VERSION_NUMBER >= 11700
		//- RenderSystem.setShader(GameRenderer::getPositionColorShader);
		//- //# end
		//- //# if MC_VERSION_NUMBER <= 11903
		//- RenderSystem.disableTexture();
		//- //# end
		//- CoatUtil.resetShaderColor();
		//- Tesselator tesselator = Tesselator.getInstance();
		//- //# if MC_VERSION_NUMBER >= 11700
		//- BufferBuilder bufferBuilder = tesselator.getBuilder();
		//- bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		//- //# else
		//- BufferBuilder bufferBuilder = tesselator.getBuilder();
		//- bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);
		//- //# end
		//- CoatUtil.addRect(bufferBuilder, scrollbarLeft, top, scrollbarRight, bottom, SCROLLBAR_BACKGROUND_COLOR);
		//- CoatUtil.addRect(bufferBuilder, scrollbarLeft, handleTop, scrollbarRight, handleTop + handleHeight, SCROLLBAR_HANDLE_SHADOW_COLOR);
		//- CoatUtil.addRect(bufferBuilder, scrollbarLeft, handleTop, scrollbarRight - 1, handleTop + handleHeight - 1, SCROLLBAR_HANDLE_COLOR);
		//- tesselator.end();
		//# end
	}

	/**
	 * Renders all visible entries.
	 *
	 * @param graphics The matrix stack to use for rendering
	 * @param mouseX   The current mouse x position
	 * @param mouseY   The current mouse y position
	 * @param delta    The tick delta
	 */
	//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
	public void renderList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
	//# elif RENDERING == "GUI_GRAPHICS"
	//- public void renderList(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
	//# else
	//- protected void renderList(PoseStack graphics, int mouseX, int mouseY, float delta) {
	//# end
		IntListIterator bottomIter = entries.bottoms.iterator();
		Iterator<E> entryIter = entries.iterator();
		int relBottom = 0, relTop = 0;
		final int entryAreaTop = getEntryAreaTop();
		E entry = null;

		while (bottomIter.hasNext()) {
			relTop = relBottom;
			relBottom = bottomIter.nextInt();
			entry = entryIter.next();
			if (entryAreaTop + relBottom > top) {
				break;
			}
		}

		E hoveredEntry = getEntryAtPosition(mouseX, mouseY);

		int rowWidth = getEntryWidth();
		int rowLeft = getEntryLeft();
		while (true) {
			if (entry == null) {
				break;
			}

			int rowTop = relTop + entryAreaTop;

			entry.render(graphics, rowLeft, rowTop, rowWidth, relBottom - relTop, mouseX, mouseY, hoveredEntry == entry, delta);

			if (bottomIter.hasNext()) {
				relTop = relBottom;
				relBottom = bottomIter.nextInt();
				entry = entryIter.next();
			} else {
				break;
			}
		}
	}

	/**
	 * Scroll to the given entry so that it's directly in the center of the screen.
	 *
	 * @param entry The entry to scroll to
	 */
	public void centerScrollOn(E entry) {
		int index = entries.indexOf(entry);
		setScrollYOffset(entries.bottoms.getInt(index) - entry.getHeight() / 2D - (bottom - top) / 2D);
	}

	/**
	 * Enforces that the given entry is visible by scrolling to it if necessary.
	 * Does nothing if the entry is already visible.
	 *
	 * @param entry The entry to make visible
	 */
	public void ensureVisible(E entry) {
		int index = entries.indexOf(entry);
		int entryBottom = entries.bottoms.getInt(index);
		if (getEntryAreaTop() + entryBottom > bottom) {
			setScrollYOffset(entryBottom - height);
		}

		int entryTop = index == 0 ? 0 : entries.bottoms.getInt(index - 1);

		if (getEntryAreaTop() + entryTop < top) {
			setScrollYOffset(entryTop);
		}
	}

	/**
	 * Scrolls by the given amount.
	 *
	 * @param amount The amount to scroll by
	 */
	private void scroll(int amount) {
		this.setScrollYOffset(this.getScrollYOffset() + amount);
	}

	/**
	 * Sets the scroll position.
	 *
	 * @param amount The new scroll position
	 */
	public void setScrollYOffset(double amount) {
		this.scrollYOffset = Mth.clamp(amount, 0.0D, this.getMaxScroll());
	}

	/**
	 * Gets the maximum scroll position.
	 *
	 * @return The highest allowed offset for scrolling
	 */
	public int getMaxScroll() {
		return Math.max(0, this.getMaxPosition() - height + TOP_PADDING);
	}

	protected void updateScrollingState(double mouseX, double mouseY, int button) {
		//# if MC_VERSION_NUMBER >= 260300
		int scrollbarX = this.getScrollbarPositionX();
		this.scrolling = (button == InputConstants.MOUSE_BUTTON_LEFT || button == 0) && mouseX >= (double) (scrollbarX - 2) && mouseX < (double) (scrollbarX + 8);
		//# else
		//- this.scrolling = button == 0 && mouseX >= (double) this.getScrollbarPositionX() && mouseX < (double) (this.getScrollbarPositionX() + 6);
		//# end
	}

	protected int getScrollbarPositionX() {
		return getEntryRight() + CoatUtil.MARGIN;
	}

	//# if MC_VERSION_NUMBER >= 12110
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		//# else
	//- public boolean mouseClicked(double mouseX, double mouseY, int button) {
	//# end
		this.updateScrollingState(mouseX, mouseY, button);
		if (!isMouseOver(mouseX, mouseY)) {
			return false;
		} else {
			Entry entry = getEntryAtPosition(mouseX, mouseY);
			if (entry != null) {
				//# if MC_VERSION_NUMBER >= 12110
				boolean handled = entry.mouseClicked(event, doubleClick);
				//# else
				//- boolean handled = entry.mouseClicked(mouseX, mouseY, button);
				//# end
				if (handled && entry.getParent() == this) {
					setFocused(entry);
					setDragging(true);
					return true;
				}
			} else {
				setFocused(null);
			}

			return scrolling;
		}
	}

	//# if MC_VERSION_NUMBER >= 12110
	public boolean mouseReleased(MouseButtonEvent event) {
		this.scrolling = false;
		if (this.getFocused() != null) {
			this.getFocused().mouseReleased(event);
		}
		return false;
	}
	//# else
	//- public boolean mouseReleased(double mouseX, double mouseY, int button) {
	//- 	if (this.getFocused() != null) {
	//- 		this.getFocused().mouseReleased(mouseX, mouseY, button);
	//- 	}
	//- 	return false;
	//- }
	//# end

	//# if MC_VERSION_NUMBER >= 12110
	public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		if (super.mouseDragged(event, deltaX, deltaY)) {
			return true;
	//# else
	//- public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
	//- 	if (super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
	//- 		return true;
	//# end
		//# if MC_VERSION_NUMBER >= 260300
		} else if (this.scrolling) {
		//# else
		//- } else if (button == 0 && this.scrolling) {
		//# end
			if (mouseY < (double) this.top) {
				this.setScrollYOffset(0.0D);
			} else if (mouseY > (double) this.bottom) {
				this.setScrollYOffset(this.getMaxScroll());
			} else {
				double d = Math.max(1, this.getMaxScroll());
				int i = this.bottom - this.top;
				int j = Mth.clamp((int) ((float) (i * i) / (float) this.getMaxPosition()), 32, i - 8);
				double e = Math.max(1.0D, d / (double) (i - j));
				this.setScrollYOffset(this.getScrollYOffset() + deltaY * e);
			}

			return true;
		} else {
			return false;
		}
	}

	@Override
	//# if SCROLL_DIRECTIONS == "BOTH"
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
	//# elif SCROLL_DIRECTIONS == "VERTICAL"
	//- public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
	//# end
		Entry entry = getEntryAtPosition(mouseX, mouseY);
		//# if SCROLL_DIRECTIONS == "BOTH"
		if (entry != null && entry.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
		//# elif SCROLL_DIRECTIONS == "VERTICAL"
		//- if (entry != null && entry.mouseScrolled(mouseX, mouseY, verticalAmount)) {
		//# end
			return true;
		}
		double prevScroll = getScrollYOffset();
		this.setScrollYOffset(this.getScrollYOffset() - verticalAmount * 10.0D);
		return getScrollYOffset() != prevScroll;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseY >= (double) this.top && mouseY <= (double) this.bottom && mouseX >= (double) this.left && mouseX <= (double) this.right;
	}

	/**
	 * Gets the left position of the entries.
	 *
	 * @return The left position of the entries
	 */
	public int getEntryLeft() {
		return this.left + this.width / 2 - this.getEntryWidth() / 2;
	}

	/**
	 * Gets the right position of the entries.
	 *
	 * @return The right position of the entries
	 */
	public int getEntryRight() {
		return this.right - this.width / 2 + this.getEntryWidth() / 2;
	}

	/**
	 * Gets the absolute position where the entry area begins.
	 *
	 * @return The top position of the first entry
	 */
	protected int getEntryAreaTop() {
		return top + TOP_PADDING - (int) scrollYOffset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setFocused(@Nullable GuiEventListener focused) {
		GuiEventListener old = getFocused();
		if (old != null && old != focused) {
			if (old instanceof Entry) {
				((Entry) old).focusLost();
			}
		}
		if (focused != null) {
			if (focused instanceof Entry) {
				//noinspection unchecked
				ensureVisible((E) focused);
			}
		}
		super.setFocused(focused);
	}

	/**
	 * Remove an entry
	 *
	 * @param entry The entry to remove
	 * @return The removed entry
	 */
	protected E removeEntry(E entry) {
		if (entry == getFocused()) {
			//# if MC_VERSION_NUMBER <= 11903
			//- changeFocus(true);
			//# else
			setFocused(true);
			//# end
		}
		entries.remove(entry);
		return entry;
	}

	/**
	 * Remove an entry by index
	 *
	 * @param index The index to remove an entry from
	 * @return The removed entry
	 */
	protected E removeEntry(int index) {
		E entry = entries.get(index);
		return removeEntry(entry);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void tick() {
		for (E child : entries) {
			child.tick();
		}
	}

	//# if MC_VERSION_NUMBER >= 11700
	@Override
	public void updateNarration(NarrationElementOutput narrationElementOutput) {
		// TODO: narrations
	}

	@Override
	public NarrationPriority narrationPriority() {
		return NarrationPriority.NONE;
	}
	//# end

	/**
	 * List class that represents the entries of an entry list widget
	 */
	@Environment(EnvType.CLIENT)
	class Entries extends AbstractList<E> {
		private final List<E> entries;
		/**
		 * A list containing all bottom positions of the entries
		 */
		protected final IntList bottoms = new IntArrayList();

		private Entries() {
			this.entries = Lists.newArrayList();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public E get(int i) {
			return this.entries.get(i);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int size() {
			return this.entries.size();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isEmpty() {
			return entries.isEmpty();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public E set(int i, E entry) {
			entry.setParent(DynamicEntryListWidget.this);
			return this.entries.set(i, entry);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void add(int i, E entry) {
			bottoms.add(0);
			entry.setParent(DynamicEntryListWidget.this);
			entries.add(i, entry);
			entryHeightChanged(entry);
			entry.widthChanged(getEntryWidth());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean addAll(@NotNull Collection<? extends E> newEntries) {
			int oldSize = entries.size();
			int bottom = bottoms.isEmpty() ? 0 : bottoms.getInt(0);
			entries.addAll(newEntries);
			for (int i = oldSize, l = entries.size(); i < l; i++) {
				bottom += entries.get(i).getHeight();
				bottoms.add(bottom);
			}
			for (E newEntry : newEntries) {
				newEntry.setParent(DynamicEntryListWidget.this);
				newEntry.widthChanged(getEntryWidth());
			}
			return true;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public E remove(int i) {
			E entry = entries.remove(i);
			entry.setParent(null);
			int height = entry.getHeight();
			bottoms.removeInt(i);
			if (entry == getFocused()) {
				setFocused(null);
			}
			for (int j = i; j < entries.size(); j++) {
				bottoms.set(j, bottoms.getInt(j) - height);
			}
			return entry;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void clear() {
			entries.clear();
			bottoms.clear();
		}
	}

	/**
	 * A dynamically sized entry in a {@link DynamicEntryListWidget}.
	 */
	@Environment(EnvType.CLIENT)
	public abstract static class Entry
			//# if RENDERING == "POSE_STACK"
			//- extends GuiComponent
			//# end
			implements GuiEventListener, TickableElement {
		/**
		 * The parent element.
		 */
		@Getter
		@Setter
		protected EntryContainer parent;

		/**
		 * Renders an entry in a list.
		 *
		 * @param graphics    the matrix stack used for rendering
		 * @param x           the X coordinate of the entry
		 * @param y           the Y coordinate of the entry
		 * @param entryWidth  the width of the entry.
		 *                    Expensive calculations based on this should be done in {@link Entry#widthChanged(int)}.
		 * @param entryHeight The height of the entry
		 * @param mouseX      the X coordinate of the mouse
		 * @param mouseY      the Y coordinate of the mouse
		 * @param hovered     whether the mouse is hovering over the entry
		 */
		//# if RENDERING == "GUI_GRAPHICS_EXTRACTOR"
		public abstract void render(GuiGraphicsExtractor graphics, int x, int y, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
		//# elif RENDERING == "GUI_GRAPHICS"
		//- public abstract void render(GuiGraphics graphics, int x, int y, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
		//# elif RENDERING == "POSE_STACK"
		//- public abstract void render(PoseStack graphics, int x, int y, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta);
		//# end

		/**
		 * The current height of this entry. Height updates should be announced via the parent's {@link EntryContainer#entryHeightChanged(GuiEventListener)}:<br />
		 * <code>getParent().entryHeightChanged(this)</code>
		 *
		 * @return The current height
		 */
		public abstract int getHeight();

		/**
		 * Called whenever the width of the parent changes.
		 *
		 * @param newWidth The new width
		 */
		public void widthChanged(int newWidth) {

		}

		/**
		 * Called when the focus on this element is lost. This can be used to clean up focuses of children.
		 */
		public void focusLost() {

		}
	}
}
