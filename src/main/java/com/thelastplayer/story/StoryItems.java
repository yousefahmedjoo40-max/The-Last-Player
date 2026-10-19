package com.thelastplayer.story;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

public final class StoryItems {

    public static ItemStack book(String title, String author, String... pages) {
        ItemStack s = new ItemStack(Items.WRITTEN_BOOK);
        CompoundNBT tag = s.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        ListNBT list = new ListNBT();
        for (String p : pages) {
            list.add(StringNBT.valueOf(ITextComponent.Serializer.toJson(new StringTextComponent(p))));
        }
        tag.put("pages", list);
        return s;
    }

    /** A compass that points at pos (no lodestone needed). */
    public static ItemStack compass(BlockPos pos, String name) {
        ItemStack s = new ItemStack(Items.COMPASS);
        CompoundNBT tag = s.getOrCreateTag();
        tag.put("LodestonePos", NBTUtil.writeBlockPos(pos));
        tag.putString("LodestoneDimension", "minecraft:overworld");
        tag.putBoolean("LodestoneTracked", false);
        s.setHoverName(new StringTextComponent(name).withStyle(TextFormatting.AQUA));
        return s;
    }

    public static String coords(BlockPos p) {
        return "X: " + p.getX() + "  Y: " + p.getY() + "  Z: " + p.getZ();
    }

    public static ItemStack signalBook(BlockPos device) {
        return book("إشارة ضعيفة", "؟؟؟",
                "وصلتك إشارة ضعيفة من تحت الأرض...\n\nشيء ما يعمل هناك منذ زمن طويل.\n\n" + coords(device),
                "الغرفة مغلقة بالكامل. ستحتاج إلى أن تحفر.\n\nاستخدم البوصلة.");
    }

    public static ItemStack labBook(BlockPos lab) {
        return book("FACILITY 07", "PROJECT ECHO",
                "ملاحظات ميدانية.\n\nمنشأة 07 — مختبر مهجور.\nالمدخل على السطح: كوخ صغير بسلّم.\n\n" + coords(lab),
                "SESSION ENDED\nSUBJECT ABANDONED\n\nإن كنت تقرأ هذا فقد وصلت متأخرًا.");
    }

    public static ItemStack logBook() {
        return book("سجل الصيانة", "د. ن.",
                "تقرير صيانة — المنشأة 07\n\nجميع الأنظمة تعمل بأقل طاقة. نواة ECHO-07 تُظهر نشاطًا غير مبرمج عند وجود لاعب متصل.",
                "لا تخبروا الإدارة.\nإنه يبدو... وحيدًا.");
    }

    public static ItemStack maintenanceHint() {
        return book("ملاحظة صيانة", "قسم الصيانة",
                "القطاع 7:\nتم ترقيع الجدار الغربي لغرفة السجلات بحجارة عادية (الكوبلستون) بعد حادثة التسرّب.\n\nخلفه غرفة صغيرة. لا تفتحوها.");
    }

    private StoryItems() {}
}
