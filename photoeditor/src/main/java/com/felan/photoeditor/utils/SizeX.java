package com.felan.photoeditor.utils;

public class SizeX {
    private final int width;
    private final int height;

    public SizeX(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeX)) return false;

        SizeX sizeX = (SizeX) o;

        if (width != sizeX.width) return false;
        return height == sizeX.height;
    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        return result;
    }
}
