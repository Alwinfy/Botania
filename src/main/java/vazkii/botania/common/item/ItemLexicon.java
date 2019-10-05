/**
 * This class was created by <Vazkii>. It's distributed as
 * part of the Botania Mod. Get the Source Code in github:
 * https://github.com/Vazkii/Botania
 *
 * Botania is Open Source and distributed under the
 * Botania License: http://botaniamod.net/license.php
 *
 * File Created @ [Jan 14, 2014, 5:53:00 PM (GMT)]
 */
package vazkii.botania.common.item;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.Rarity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import vazkii.botania.api.BotaniaAPI;
import vazkii.botania.api.lexicon.ILexicon;
import vazkii.botania.api.lexicon.ILexiconable;
import vazkii.botania.api.lexicon.KnowledgeType;
import vazkii.botania.api.lexicon.LexiconEntry;
import vazkii.botania.api.recipe.IElvenItem;
import vazkii.botania.client.gui.lexicon.GuiLexicon;
import vazkii.botania.common.Botania;
import vazkii.botania.common.advancements.UseItemSuccessTrigger;
import vazkii.botania.common.core.handler.ModSounds;
import vazkii.botania.common.core.helper.ItemNBTHelper;
import vazkii.botania.common.core.helper.MathHelper;
import vazkii.botania.common.core.helper.PlayerHelper;
import vazkii.botania.common.item.relic.ItemDice;
import vazkii.botania.common.lib.LibItemNames;
import vazkii.botania.common.lib.LibMisc;
import vazkii.patchouli.common.base.PatchouliSounds;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;
import vazkii.patchouli.common.network.NetworkHandler;
import vazkii.patchouli.common.network.message.MessageOpenBookGui;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ItemLexicon extends ItemMod implements ILexicon, IElvenItem {

	private static final String TAG_KNOWLEDGE_PREFIX = "knowledge.";
	private static final String TAG_FORCED_MESSAGE = "forcedMessage";
	private static final String TAG_QUEUE_TICKS = "queueTicks";
	private boolean skipSound = false;

	public ItemLexicon(Properties props) {
		super(props);
		addPropertyOverride(new ResourceLocation(LibMisc.MOD_ID, "elven"), (stack, world, living) -> isElvenItem(stack) ? 1 : 0);
	}

	private static Book getBook() {
		return BookRegistry.INSTANCE.books.get(ModItems.lexicon.getRegistryName());
	}

	/*
	@Nonnull
	@Override
	public ActionResultType onItemUse(ItemUseContext ctx) {
		PlayerEntity player = ctx.getPlayer();
		if(player == null)
			return ActionResultType.PASS;

		World world = ctx.getWorld();
		BlockPos pos = ctx.getPos();
		if(player.isSneaking()) {
			Block block = world.getBlockState(pos).getBlock();

			if(block instanceof ILexiconable) {
				ItemStack stack = ctx.getItem();
				LexiconEntry entry = ((ILexiconable) block).getEntry(world, pos, player, stack);
				if(entry != null && isKnowledgeUnlocked(stack, entry.getKnowledgeType())) {
					Botania.proxy.setEntryToOpen(entry);
					Botania.proxy.setLexiconStack(stack);

					openBook(player, stack, world, false);
					return ActionResultType.SUCCESS;
				}
			} else if(world.isRemote) {
				BlockRayTraceResult mop = new BlockRayTraceResult(ctx.getHitVec(), ctx.getFace(), pos, ctx.func_221533_k());
				return Botania.proxy.openWikiPage(world, block, mop) ? ActionResultType.SUCCESS : ActionResultType.FAIL;
			}
		}

		return ActionResultType.PASS;
	}

	@Override
	public void fillItemGroup(@Nonnull ItemGroup tab, @Nonnull NonNullList<ItemStack> list) {
		if(isInGroup(tab)) {
			list.add(new ItemStack(this));
			ItemStack creative = new ItemStack(this);
			for(String s : BotaniaAPI.knowledgeTypes.keySet()) {
				KnowledgeType type = BotaniaAPI.knowledgeTypes.get(s);
				unlockKnowledge(creative, type);
			}
			list.add(creative);
		}
	}
	*/

	@Override
	@OnlyIn(Dist.CLIENT)
	public void addInformation(ItemStack stack, World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
		super.addInformation(stack, worldIn, tooltip, flagIn);

		if (Screen.hasShiftDown()) {
			Book book = getBook();
			if(book.contents != null)
				tooltip.add(new StringTextComponent(book.contents.getSubtitle()).applyTextStyle(TextFormatting.GRAY));
		} else {
			tooltip.add(new TranslationTextComponent("botaniamisc.shiftinfo"));
		}
	}

	@Nonnull
	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack stack = playerIn.getHeldItem(handIn);
		Book book = getBook();

		if(playerIn instanceof ServerPlayerEntity) {
			NetworkHandler.sendToPlayer(new MessageOpenBookGui(book.resourceLoc.toString()), (ServerPlayerEntity) playerIn);
			SoundEvent sfx = PatchouliSounds.getSound(book.openSound, PatchouliSounds.book_open);
			worldIn.playSound(null, playerIn.posX, playerIn.posY, playerIn.posZ, sfx, SoundCategory.PLAYERS, 1F, (float) (0.7 + Math.random() * 0.4));
		}

		return new ActionResult<>(ActionResultType.SUCCESS, stack);
	}

	@OnlyIn(Dist.CLIENT)
	public static String getEdition() {
		String version = getBook().version;
		try {
			return MathHelper.numberToOrdinal(Integer.parseInt(version));
		} catch (NumberFormatException e) {
			return I18n.format("botaniamisc.devEdition");
		}
	}

	public static void openBook(PlayerEntity player, ItemStack stack, World world, boolean skipSound) {
		ILexicon l = (ILexicon) stack.getItem();

		Botania.proxy.setToTutorialIfFirstLaunch();

		if(!l.isKnowledgeUnlocked(stack, BotaniaAPI.relicKnowledge) && l.isKnowledgeUnlocked(stack, BotaniaAPI.elvenKnowledge))
			for(Item item : ItemDice.getRelics()) {
				if(PlayerHelper.hasItem(player, s -> s != null && s.getItem() == item)) {
					l.unlockKnowledge(stack, BotaniaAPI.relicKnowledge);
					break;
				}
			}

		Botania.proxy.setLexiconStack(stack);
		if(world.isRemote)
			DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().displayGuiScreen(GuiLexicon.currentOpenLexicon));
		if(!world.isRemote) {
			if(!skipSound)
				world.playSound(null, player.posX, player.posY, player.posZ, ModSounds.lexiconOpen, SoundCategory.PLAYERS, 0.5F, 1F);
			UseItemSuccessTrigger.INSTANCE.trigger((ServerPlayerEntity) player, stack, (ServerWorld) world, player.posX, player.posY, player.posZ);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int idk, boolean something) {
		int ticks = getQueueTicks(stack);
		if(ticks > 0 && entity instanceof PlayerEntity) {
			skipSound = ticks < 5;
			if(ticks == 1)
				onItemRightClick(world, (PlayerEntity) entity, Hand.MAIN_HAND);

			setQueueTicks(stack, ticks - 1);
		}
	}

	@Nonnull
	@Override
	public Rarity getRarity(ItemStack par1ItemStack) {
		return Rarity.UNCOMMON;
	}

	@Override
	public boolean isKnowledgeUnlocked(ItemStack stack, KnowledgeType knowledge) {
		return knowledge.autoUnlock || ItemNBTHelper.getBoolean(stack, TAG_KNOWLEDGE_PREFIX + knowledge.id, false);
	}

	@Override
	public void unlockKnowledge(ItemStack stack, KnowledgeType knowledge) {
		ItemNBTHelper.setBoolean(stack, TAG_KNOWLEDGE_PREFIX + knowledge.id, true);
	}

	public static void setForcedPage(ItemStack stack, String forced) {
		ItemNBTHelper.setString(stack, TAG_FORCED_MESSAGE, forced);
	}

	public static String getForcedPage(ItemStack stack) {
		return ItemNBTHelper.getString(stack, TAG_FORCED_MESSAGE, "");
	}

	private static LexiconEntry getEntryFromForce(ItemStack stack) {
		String force = getForcedPage(stack);

		for(LexiconEntry entry : BotaniaAPI.getAllEntries())
			if(entry.getUnlocalizedName().equals(force))
				if(((ItemLexicon) stack.getItem()).isKnowledgeUnlocked(stack, entry.getKnowledgeType()))
					return entry;

		return null;
	}

	public static int getQueueTicks(ItemStack stack) {
		return ItemNBTHelper.getInt(stack, TAG_QUEUE_TICKS, 0);
	}

	public static void setQueueTicks(ItemStack stack, int ticks) {
		ItemNBTHelper.setInt(stack, TAG_QUEUE_TICKS, ticks);
	}

	public static ITextComponent getTitle(ItemStack stack) {
		ITextComponent title = stack.getDisplayName();

		String akashicTomeNBT = "akashictome:displayName";
		if(stack.hasTag() && stack.getTag().contains(akashicTomeNBT)) {
			title = new StringTextComponent(stack.getTag().getString(akashicTomeNBT));
		}
		
		return title;
	}
	
	@Override
	public boolean isElvenItem(ItemStack stack) {
		return isKnowledgeUnlocked(stack, BotaniaAPI.elvenKnowledge);
	}
}
