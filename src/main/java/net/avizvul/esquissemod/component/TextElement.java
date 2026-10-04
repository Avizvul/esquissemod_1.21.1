package net.avizvul.esquissemod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TextElement(String text, int x, int y, float scale, int color, boolean isVertical) {

    public TextElement(String text, int x, int y, float scale, int color) {
        this(text, x, y, scale, color, false);
    }

    public static final Codec<TextElement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("text").forGetter(TextElement::text),
                    Codec.INT.fieldOf("x").forGetter(TextElement::x),
                    Codec.INT.fieldOf("y").forGetter(TextElement::y),
                    Codec.FLOAT.fieldOf("scale").forGetter(TextElement::scale),
                    Codec.INT.fieldOf("color").forGetter(TextElement::color),
                    Codec.BOOL.optionalFieldOf("is_vertical", false).forGetter(TextElement::isVertical)
            ).apply(instance, TextElement::new)
    );

    public static final StreamCodec<ByteBuf, TextElement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TextElement::text,
            ByteBufCodecs.INT, TextElement::x,
            ByteBufCodecs.INT, TextElement::y,
            ByteBufCodecs.FLOAT, TextElement::scale,
            ByteBufCodecs.INT, TextElement::color,
            ByteBufCodecs.BOOL, TextElement::isVertical,
            TextElement::new
    );
}
