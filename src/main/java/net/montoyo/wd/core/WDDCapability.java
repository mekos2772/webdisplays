package net.montoyo.wd.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class WDDCapability implements IWDDCapability {
    public static final AttachmentType<IWDDCapability> ATTACHMENT =
        AttachmentType.builder(() -> (IWDDCapability) new WDDCapability())
            .serialize(new IAttachmentSerializer<CompoundTag, IWDDCapability>() {
                @Override
                public IWDDCapability read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
                    WDDCapability cap = new WDDCapability();
                    if (tag.contains("firstRun"))
                        cap.firstRun = tag.getBoolean("firstRun");
                    return cap;
                }

                @Override
                @Nullable
                public CompoundTag write(IWDDCapability attachment, HolderLookup.Provider provider) {
                    CompoundTag tag = new CompoundTag();
                    tag.putBoolean("firstRun", attachment.isFirstRun());
                    return tag;
                }
            })
            .build();

    private boolean firstRun = true;

    public WDDCapability() {
    }

    @Override
    public boolean isFirstRun() {
        return firstRun;
    }

    @Override
    public void clearFirstRun() {
        firstRun = false;
    }

    @Override
    public void cloneTo(IWDDCapability dst) {
        if (!isFirstRun())
            dst.clearFirstRun();
    }
}
