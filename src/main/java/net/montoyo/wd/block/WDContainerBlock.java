/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.BaseEntityBlock;
import com.mojang.serialization.MapCodec;

public abstract class WDContainerBlock extends BaseEntityBlock {

    protected static BlockItem itemBlock;

    public WDContainerBlock(Properties arg) {
        super(arg);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    public BlockItem getItem() {
        return itemBlock;
    }

}
