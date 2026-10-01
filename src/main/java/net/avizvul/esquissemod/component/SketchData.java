package net.avizvul.esquissemod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SketchData {

    private final int[][] pixels;
    private final List<TextElement> textElements;
    private final int cachedHashCode;

    // Стрим-кодек для списка текстовых элементов
    private static final StreamCodec<ByteBuf, List<TextElement>> TEXT_LIST_CODEC =
            ByteBufCodecs.collection(ArrayList::new, TextElement.STREAM_CODEC);

    // --- CODEC ДЛЯ СОХРАНЕНИЯ В NBT / ФАЙЛЫ МИРА ---

    private static final Codec<int[][]> PIXELS_CODEC = Codec.INT_STREAM.xmap(
    stream -> {
            int[] arr = stream.toArray();
        int w = 126;
        int h = 192;
            int[][] pixels2D = new int[w][h];
        if (arr.length == w * h) {
            for (int x = 0; x < w; x++) {
                System.arraycopy(arr, x * h, pixels2D[x], 0, h);
            }
        }
        return pixels2D;
    },
    pixels2D -> {
        int w = 126;
        int h = 192;
            int[] arr = new int[w * h];
        for (int x = 0; x < w; x++) {
            if (x < pixels2D.length && pixels2D[x] != null) {
                int copyH = Math.min(h, pixels2D[x].length);
                System.arraycopy(pixels2D[x], 0, arr, x * h, copyH);
            }
        }
        return Arrays.stream(arr);
    }
    );

    public static final Codec<SketchData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    PIXELS_CODEC.fieldOf("pixels").forGetter(SketchData::getRawPixels),
                    TextElement.CODEC.listOf().optionalFieldOf("text_elements", new ArrayList<>()).forGetter(SketchData::getTextElements)
            ).apply(instance, SketchData::new)
    );

    // --- STREAM CODEC ДЛЯ ОПТИМИЗИРОВАННОЙ СЕТЕВОЙ ПЕРЕДАЧИ (GZIP + TEXTS) ---

    public static final StreamCodec<ByteBuf, SketchData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                int w = 126;
                int h = 192;
            int[][] pixels2D = data.toArray(w, h);

                // 1. Сжатие и запись пиксельного массива
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    GZIPOutputStream gzip = new GZIPOutputStream(baos);
                    DataOutputStream dos = new DataOutputStream(gzip);

                    for (int x = 0; x < w; x++) {
                        for (int y = 0; y < h; y++) {
                            dos.writeInt(pixels2D[x][y]);
                        }
                    }
                    dos.flush();
                    gzip.finish();

                byte[] compressed = baos.toByteArray();
                    buf.writeInt(compressed.length);
                    buf.writeBytes(compressed);
                } catch (Exception e) {
                    buf.writeInt(0);
                    e.printStackTrace();
                }

                // 2. Сетевая запись векторных текстовых элементов
                TEXT_LIST_CODEC.encode(buf, data.getTextElements());
            },
            buf -> {
                int w = 126;
                int h = 192;
            int[][] pixels2D = new int[w][h];

                // 1. Чтение и распаковка пиксельного массива
                int len = buf.readInt();
                if (len > 0) {
                byte[] compressed = new byte[len];
                    buf.readBytes(compressed);

                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
                        GZIPInputStream gzip = new GZIPInputStream(bais);
                        DataInputStream dis = new DataInputStream(gzip);

                        for (int x = 0; x < w; x++) {
                            for (int y = 0; y < h; y++) {
                                pixels2D[x][y] = dis.readInt();
                            }
                        }
                        dis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // 2. Сетевое чтение векторных текстовых элементов
                List<TextElement> texts = TEXT_LIST_CODEC.decode(buf);

                return new SketchData(pixels2D, texts);
            }
    );

    // --- КОНСТРУКТОРЫ И ФАБРИКИ ---

    public SketchData(int[][] pixels, List<TextElement> textElements) {
        this.pixels = pixels != null ? pixels : new int[0][0];
        this.textElements = textElements != null ? new ArrayList<>(textElements) : new ArrayList<>();
        this.cachedHashCode = Objects.hash(Arrays.deepHashCode(this.pixels), this.textElements);
    }

    public SketchData(int[][] pixels) {
        this(pixels, new ArrayList<>());
    }

    public static SketchData fromArray(int[][] arr) {
        int w = arr.length;
        if (w == 0) return new SketchData(new int[0][0], new ArrayList<>());

        int h = arr[0].length;
        int[][] copy = new int[w][h];

        for (int x = 0; x < w; x++) {
            System.arraycopy(arr[x], 0, copy[x], 0, h);
        }
        return new SketchData(copy, new ArrayList<>());
    }

    public static SketchData fromArrayAndTexts(int[][] arr, List<TextElement> texts) {
        int w = arr.length;
        if (w == 0) return new SketchData(new int[0][0], texts);

        int h = arr[0].length;
        int[][] copy = new int[w][h];

        for (int x = 0; x < w; x++) {
            System.arraycopy(arr[x], 0, copy[x], 0, h);
        }
        return new SketchData(copy, texts);
    }

    // --- ГЕТТЕРЫ И ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    public int[][] getRawPixels() {
        return this.pixels;
    }

    public List<TextElement> getTextElements() {
        return this.textElements;
    }

    public int[][] toArray(int targetWidth, int targetHeight) {
        int[][] result = new int[targetWidth][targetHeight];
        int copyWidth = Math.min(targetWidth, this.pixels.length);

        if (copyWidth > 0 && this.pixels[0] != null) {
            int copyHeight = Math.min(targetHeight, this.pixels[0].length);

            for (int x = 0; x < copyWidth; x++) {
                System.arraycopy(this.pixels[x], 0, result[x], 0, copyHeight);
            }
        }
        return result;
    }

    public boolean isEmpty() {
        boolean noPixels = true;
        for (int[] row : this.pixels) {
            for (int p : row) {
                if (p != 0) {
                    noPixels = false;
                    break;
                }
            }
            if (!noPixels) break;
        }
        return noPixels && this.textElements.isEmpty();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SketchData that = (SketchData) obj;
        if (this.cachedHashCode != that.cachedHashCode) return false;
        return Arrays.deepEquals(this.pixels, that.pixels) && Objects.equals(this.textElements, that.textElements);
    }

    @Override
    public int hashCode() {
        return this.cachedHashCode;
    }
}
