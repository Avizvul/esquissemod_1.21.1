package net.avizvul.esquissemod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record TextElement(String text, int x, int y, float scale, int color, float rotation) {

    public TextElement(String text, int x, int y, float scale, int color) {
        this(text, x, y, scale, color, 0.0f);
    }

    public TextElement(String text, int x, int y, float scale, int color, boolean isVertical) {
        this(text, x, y, scale, color, isVertical ? 90.0f : 0.0f);
    }

    public boolean isVertical() {
        return Math.abs(this.rotation % 180.0f - 90.0f) < 1.0f;
    }

    public static final Codec<TextElement> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("text").forGetter(TextElement::text),
                    Codec.INT.fieldOf("x").forGetter(TextElement::x),
                    Codec.INT.fieldOf("y").forGetter(TextElement::y),
                    Codec.FLOAT.fieldOf("scale").forGetter(TextElement::scale),
                    Codec.INT.fieldOf("color").forGetter(TextElement::color),
                    Codec.FLOAT.optionalFieldOf("rotation", 0.0f).forGetter(TextElement::rotation)
            ).apply(instance, TextElement::new)
    );

    public static final StreamCodec<ByteBuf, TextElement> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TextElement::text,
            ByteBufCodecs.INT, TextElement::x,
            ByteBufCodecs.INT, TextElement::y,
            ByteBufCodecs.FLOAT, TextElement::scale,
            ByteBufCodecs.INT, TextElement::color,
            ByteBufCodecs.FLOAT, TextElement::rotation,
            TextElement::new
    );
}
