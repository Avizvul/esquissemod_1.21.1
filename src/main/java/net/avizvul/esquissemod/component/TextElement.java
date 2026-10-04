package net.avizvul.esquissemod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TextElement(String text, int x, int y, float scale, int color, int rotation) {

    public TextElement(String text, int x, int y, float scale, int color) {
        this(text, x, y, scale, color, 0);
    }

    public TextElement(String text, int x, int y, float scale, int color, boolean isVertical) {
        this(text, x, y, scale, color, isVertical ? 1 : 0);
    }

    public boolean isVertical() {
        return this.rotation % 2 != 0;
    }

    public static final Codec<TextElement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("text").forGetter(TextElement::text),
                    Codec.INT.fieldOf("x").forGetter(TextElement::x),
                    Codec.INT.fieldOf("y").forGetter(TextElement::y),
                    Codec.FLOAT.fieldOf("scale").forGetter(TextElement::scale),
                    Codec.INT.fieldOf("color").forGetter(TextElement::color),
                    Codec.INT.optionalFieldOf("rotation", 0).forGetter(TextElement::rotation)
            ).apply(instance, TextElement::new)
    );

    public static final StreamCodec<ByteBuf, TextElement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TextElement::text,
            ByteBufCodecs.INT, TextElement::x,
            ByteBufCodecs.INT, TextElement::y,
            ByteBufCodecs.FLOAT, TextElement::scale,
            ByteBufCodecs.INT, TextElement::color,
            ByteBufCodecs.INT, TextElement::rotation,
            TextElement::new
    );
}
