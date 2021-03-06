package net.silentchaos512.gems.block;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.silentchaos512.gems.SilentGems;
import net.silentchaos512.gems.init.ModBlocks;
import net.silentchaos512.gems.lib.EnumGem;
import net.silentchaos512.gems.lib.Names;
import net.silentchaos512.lib.registry.RecipeMaker;
import net.silentchaos512.lib.util.LocalizationHelper;
import net.silentchaos512.wit.api.IWitHudInfo;

public class BlockGemLamp extends BlockGemSubtypes implements IWitHudInfo {

  private final boolean lit;
  private final boolean inverted;

  public BlockGemLamp(boolean dark, boolean lit, boolean inverted) {

    super(16, dark,
        Names.GEM_LAMP + (lit ? "Lit" : "") + (inverted ? "Inverted" : "") + (dark ? "Dark" : ""),
        Material.REDSTONE_LIGHT);

    this.lit = lit;
    this.inverted = inverted;

    setHardness(0.3f);
    setResistance(10.0f);
    setLightLevel(lit ? 1 : 0);
  }

  public static BlockGemLamp getLamp(boolean dark, boolean lit, boolean inverted) {

    switch ((dark ? 4 : 0) | (lit ? 2 : 0) | (inverted ? 1 : 0)) {
      // @formatter:off
      case 0: return ModBlocks.gemLamp;
      case 1: return ModBlocks.gemLampInverted;
      case 2: return ModBlocks.gemLampLit;
      case 3: return ModBlocks.gemLampLitInverted;
      case 4: return ModBlocks.gemLampDark;
      case 5: return ModBlocks.gemLampInvertedDark;
      case 6: return ModBlocks.gemLampLitDark;
      case 7: return ModBlocks.gemLampLitInvertedDark;
      // @formatter:on
    }
    return null;
  }

  public BlockGemLamp getLamp(boolean isLit) {

    return getLamp(this.isDark, isLit, this.inverted);
  }

  private void setState(World world, BlockPos pos, BlockGemLamp block, EnumGem gem) {

    world.setBlockState(pos, block.getDefaultState().withProperty(EnumGem.VARIANT_GEM, gem), 2);
  }

  @Override
  public void onBlockAdded(World world, BlockPos pos, IBlockState state) {

    if (!world.isRemote) {
      boolean powered = world.isBlockPowered(pos);
      EnumGem gem = EnumGem.values()[getMetaFromState(state)];
      setState(world, pos, getLamp(inverted ? !powered : powered), gem);
    }
  }

  @Override
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn,
      BlockPos fromPos) {

    // IBlockState state = world.getBlockState(pos);

    if (!world.isRemote) {
      boolean powered = world.isBlockPowered(pos);
      EnumGem gem = EnumGem.values()[getMetaFromState(state)];
      BlockGemLamp newBlock = getLamp(inverted ? !powered : powered);

      // String debug = "powered = %s, isDark = %s, lit = %s, inverted = %s, oldBlock = %s, newBlock = %s";
      // debug = String.format(debug, powered, isDark, lit, inverted, this, newBlock);
      // SilentGems.logHelper.debug(debug, newBlock.lit, newBlock.inverted, newBlock.isDark);

      if (inverted) {
        if (!lit && !powered) {
          world.scheduleUpdate(pos, this, 4);
        } else if (lit && powered) {
          setState(world, pos, newBlock, gem);
        }
      } else {
        if (lit && !powered) {
          world.scheduleUpdate(pos, this, 4);
        } else if (!lit && powered) {
          setState(world, pos, newBlock, gem);
        }
      }
    }
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {

    if (!world.isRemote) {
      boolean powered = world.isBlockPowered(pos);
      if (!powered) {
        EnumGem gem = EnumGem.values()[getMetaFromState(state)];
        setState(world, pos, getLamp(inverted ? !powered : powered), gem);
      }
    }
  }

  @Override
  public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {

    return false;
  }

  @Override
  public Item getItemDropped(IBlockState state, Random random, int fortune) {

    BlockGemLamp block = getLamp(isDark, inverted, inverted);
    return Item.getItemFromBlock(block);
  }

  @Override
  protected ItemStack getSilkTouchDrop(IBlockState state) {

    return new ItemStack(getItemDropped(state, SilentGems.instance.random, 0));
  }

  @Override
  public void addRecipes(RecipeMaker recipes) {

    if (!lit && !inverted) {
      // Normal lamps
      for (int i = 0; i < 16; ++i) {
        recipes.addSurroundOre(blockName + i, new ItemStack(this, 1, i), getGem(i).getItemOreName(),
            "dustRedstone", "dustGlowstone");
      }
    } else if (lit && inverted) {
      // Inverted lamps
      ItemStack redstoneTorch = new ItemStack(Blocks.REDSTONE_TORCH);
      for (int i = 0; i < 16; ++i) {
        recipes.addShapeless(blockName + i + "_invert", new ItemStack(this, 1, i),
            new ItemStack(getLamp(isDark, false, false), 1, i), redstoneTorch);
      }
    }
  }

  @Override
  public void clAddInformation(ItemStack stack, World world, List<String> list, boolean advanced) {

    if (inverted)
      list.add(SilentGems.instance.localizationHelper.getBlockSubText(Names.GEM_LAMP, "inverted"));
  }

  @Override
  public List<String> getWitLines(IBlockState state, BlockPos pos, EntityPlayer player,
      boolean advanced) {

    LocalizationHelper loc = SilentGems.instance.localizationHelper;
    String line = inverted ? loc.getBlockSubText(Names.GEM_LAMP, "inverted") : "";
    line += lit ? (line.isEmpty() ? "" : ", ") + loc.getBlockSubText(Names.GEM_LAMP, "lit") : "";
    return line.isEmpty() ? null : Lists.newArrayList(line);
  }
}
