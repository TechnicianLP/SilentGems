package net.silentchaos512.gems.item;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.api.IBlockPlacer;
import net.silentchaos512.gems.util.NBTHelper;
import net.silentchaos512.lib.item.ItemSL;
import net.silentchaos512.lib.util.ChatHelper;
import net.silentchaos512.lib.util.ItemHelper;
import net.silentchaos512.lib.util.LocalizationHelper;
import net.silentchaos512.lib.util.PlayerHelper;
import net.silentchaos512.lib.util.StackHelper;

public abstract class ItemBlockPlacer extends ItemSL implements IBlockPlacer {

  protected static final int ABSORB_DELAY = 20;
  protected static final String NBT_AUTO_FILL = "AutoFill";

  public ItemBlockPlacer(String name, int maxDamage) {

    super(1, SilentGems.MODID, name);
    setMaxDamage(maxDamage);
    setNoRepair();
    setMaxStackSize(1);
    setUnlocalizedName(name);
  }

  @Override
  public void clAddInformation(ItemStack stack, World world, List list, boolean advanced) {

    String blockPlacer = "BlockPlacer";
    LocalizationHelper loc = SilentGems.localizationHelper;

    boolean autoFillOn = getAutoFillMode(stack);
    int currentBlocks = getRemainingBlocks(stack);
    int maxBlocks = stack.getMaxDamage();

    list.add(loc.getItemSubText(blockPlacer, "count", currentBlocks, maxBlocks));
    String onOrOff = loc.getMiscText("state." + (autoFillOn ? "on" : "off"));
    list.add(loc.getItemSubText(blockPlacer, "autoFill", onOrOff));
  }

  @Override
  public abstract @Nullable IBlockState getBlockPlaced(ItemStack stack);

  public int getBlockMetaDropped(ItemStack stack) {

    IBlockState state = getBlockPlaced(stack);
    return state.getBlock().getMetaFromState(state);
  }

  @Override
  public int getRemainingBlocks(ItemStack stack) {

    return stack.getMaxDamage() - stack.getItemDamage();
  }

  public boolean getAutoFillMode(ItemStack stack) {

    if (!NBTHelper.hasKey(stack, NBT_AUTO_FILL)) {
      NBTHelper.setTagBoolean(stack, NBT_AUTO_FILL, true);
    }
    return NBTHelper.getTagBoolean(stack, NBT_AUTO_FILL);
  }

  public void setAutoFillMode(ItemStack stack, boolean value) {

    NBTHelper.setTagBoolean(stack, NBT_AUTO_FILL, value);
  }

  @Override
  public void onUpdate(ItemStack stack, World world, Entity entity, int itemSlot,
      boolean isSelected) {

    if (!world.isRemote && world.getTotalWorldTime() % ABSORB_DELAY == 0) {
      if (entity instanceof EntityPlayer && getAutoFillMode(stack)) {
        absorbBlocksFromPlayer(stack, (EntityPlayer) entity);
      }
    }
  }

  protected ItemStack absorbBlocksFromPlayer(ItemStack stack, EntityPlayer player) {

    if (stack.getItemDamage() == 0) {
      return stack;
    }

    IBlockState statePlaced = getBlockPlaced(stack);
    if (statePlaced == null)
      return stack;
    Block blockPlaced = statePlaced.getBlock();
    int metaDropped = getBlockMetaDropped(stack);
    Item itemBlock = Item.getItemFromBlock(blockPlaced);

    for (ItemStack invStack : PlayerHelper.getNonEmptyStacks(player, true, true, false)) {
      if (invStack.getItem() == itemBlock && invStack.getItemDamage() == metaDropped) {
        int damage = stack.getItemDamage();

        // Decrease damage of block placer, reduce stack size of block stack.
        if (damage - StackHelper.getCount(invStack) < 0) {
          stack.setItemDamage(0);
          StackHelper.shrink(invStack, damage);
          return stack;
        } else {
          stack.setItemDamage(damage - StackHelper.getCount(invStack));
          StackHelper.setCount(invStack, 0);
        }

        // Remove empty stacks.
        if (StackHelper.getCount(invStack) <= 0) {
          PlayerHelper.removeItem(player, invStack);
        }
      }
    }

    return stack;
  }

  public int absorbBlocks(ItemStack placer, ItemStack blockStack) {

    // TODO
    return 0;
  }

  @Override
  protected ActionResult<ItemStack> clOnItemRightClick(World world, EntityPlayer player,
      EnumHand hand) {

    ItemStack stack = player.getHeldItem(hand);
    if (!player.world.isRemote && player.isSneaking()) {
      boolean mode = !getAutoFillMode(stack);
      setAutoFillMode(stack, mode);

      LocalizationHelper loc = SilentGems.localizationHelper;
      String onOrOff = loc.getMiscText("state." + (mode ? "on" : "off"));
      onOrOff = (mode ? TextFormatting.GREEN : TextFormatting.RED) + onOrOff;
      String line = loc.getItemSubText("BlockPlacer", "autoFill", onOrOff);
      ChatHelper.sendStatusMessage(player, line, true);
    }
    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  protected EnumActionResult clOnItemUse(EntityPlayer player, World world, BlockPos pos,
      EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {

    ItemStack stack = player.getHeldItem(hand);
    if (stack.getItemDamage() == getMaxDamage(stack) && !player.capabilities.isCreativeMode) {
      return EnumActionResult.PASS; // Empty and not in creative mode.
    }

    // Create fake block stack and use it.
    IBlockState state = getBlockPlaced(stack);
    if (state == null)
      return EnumActionResult.PASS;
    Block block = state.getBlock();
    ItemStack fakeBlockStack = new ItemStack(block, 1, block.getMetaFromState(state));

    // In 1.11, we must place the fake stack in the player's hand!
    ItemStack currentOffhand = player.getHeldItemOffhand();
    player.setHeldItem(EnumHand.OFF_HAND, fakeBlockStack);

    // Use the fake stack.
    EnumActionResult result = ItemHelper.onItemUse(fakeBlockStack.getItem(), player, world, pos,
        EnumHand.OFF_HAND, facing, hitX, hitY, hitZ);

    // Return the player's offhand stack.
    player.setHeldItem(EnumHand.OFF_HAND, currentOffhand);

    if (result == EnumActionResult.SUCCESS) {
      stack.damageItem(1, player);
    }
    return result;
  }

  @Override
  public ActionResult<ItemStack> onItemLeftClickSL(World world, EntityPlayer player,
      EnumHand hand) {

    ItemStack stack = player.getHeldItem(hand);
    if (!player.world.isRemote && player.isSneaking() && getRemainingBlocks(stack) > 0) {
      // Get the block this placer stores.
      IBlockState state = getBlockPlaced(stack);
      int meta = getBlockMetaDropped(stack);

      // Create block stack to drop.
      ItemStack toDrop = new ItemStack(state.getBlock(), 1, meta);
      StackHelper.setCount(toDrop, Math.min(getRemainingBlocks(stack), toDrop.getMaxStackSize()));
      stack.damageItem(StackHelper.getCount(toDrop), player);

      // Make the EntityItem and spawn in world.
      Vec3d vec = player.getLookVec().scale(2.0);
      EntityItem entity = new EntityItem(world, player.posX + vec.x, player.posY + 1 + vec.y,
          player.posZ + vec.x, toDrop);
      vec = vec.scale(-0.125);
      entity.motionX = vec.x;
      entity.motionY = vec.y;
      entity.motionZ = vec.z;
      world.spawnEntity(entity);
    }

    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
  }

  @Override
  protected void clGetSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {

    if (!ItemHelper.isInCreativeTab(item, tab))
      return;

    ItemStack stack = new ItemStack(item);
    list.add(new ItemStack(item, 1, getMaxDamage(stack)));
    list.add(stack);
  }
}
