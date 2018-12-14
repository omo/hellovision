
#include <vector>
#include "hello/helpers/io.h"
#include "hello/helpers/trace.h"
#include "png.h"
#include <iostream>
#include <fstream>

namespace hv {

namespace {

class WriteBuffer {
public:
    static void write_callback(png_structp png, png_bytep data, png_size_t length) {
        auto self = reinterpret_cast<WriteBuffer*>(png_get_io_ptr(png));
        self->write(data, length);
    }

    static void flush_none(png_structp unused) { }

    void write(png_bytep data, png_size_t length) {
        bytes_.insert(bytes_.end(), data, data + length);
    }

    const uint8_t* data() const { return bytes_.data(); }

    size_t bytes() const { return bytes_.size(); }
private:
    std::vector<uint8_t> bytes_;
};

void handle_error(png_structp png_ptr, png_const_charp error_msg) {
    std::cerr << error_msg << std::endl;
}

class Reader {
public:
    Reader(std::istream& ins) : ins_{ins} {}

    static void read_callback(png_structp png, png_bytep data, png_size_t bytes) {
        auto self = reinterpret_cast<Reader*>(png_get_io_ptr(png));
        self->read(data, bytes);
    }

    void read(png_bytep data, png_size_t bytes) {
        ins_.read(reinterpret_cast<char*>(data), bytes);
    }

private:
    std::istream& ins_;
};

} // namespace

void write_png(const std::string& filename, const Image<uint8_t, 3>& image) {
    hv::Trace t("write_png");
    // TODO(morrita): Get the depth it from Image somwhow.
    int depth = 8;

    png_structp png = png_create_write_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    png_infop info = png_create_info_struct(png);
    png_set_error_fn(png, NULL, handle_error, handle_error);
    WriteBuffer buffer;
    png_set_write_fn(png, &buffer, &WriteBuffer::write_callback, &WriteBuffer::flush_none);

    png_set_IHDR(
        png, info, image.width(), image.height(), depth,
        PNG_COLOR_TYPE_RGB, PNG_INTERLACE_NONE, PNG_COMPRESSION_TYPE_DEFAULT, PNG_FILTER_TYPE_DEFAULT);
    png_set_gAMA(png, info, 1.0);
    png_set_compression_level(png, 0);


    png_byte** rows = reinterpret_cast<png_byte**>(png_malloc(png, image.height() * sizeof(png_byte *)));

    for (size_t y = 0; y < image.height(); ++y) {
        rows[y] = reinterpret_cast<png_byte *>(const_cast<uint8_t*>(image.row(y)));
    }

    png_set_rows(png, info, rows);
    png_write_png(png, info, PNG_TRANSFORM_IDENTITY, NULL);

    png_free(png, rows);
    png_destroy_write_struct(&png, &info);

    hv::Trace tt("write_png/write");
    std::ofstream out{filename};
    out.write(reinterpret_cast<const char*>(buffer.data()), buffer.bytes());
}

RgbImage read_png(const std::string& filename) {
    hv::Trace t("read_png");
    png_structp png = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL, NULL, NULL);
    png_infop info = png_create_info_struct(png);
    png_infop end_info = png_create_info_struct(png);
    png_set_error_fn(png, NULL, handle_error, handle_error);
    std::ifstream ins{filename};
    Reader reader{ins};
    png_set_read_fn(png, &reader, &Reader::read_callback);

    png_read_info(png, info);

    int depth  = png_get_bit_depth(png, info);
    if(depth != 8) {
        std::cerr << "Unsupported depth:" << depth << std::endl;
        abort();
    }

    int type = png_get_color_type(png, info);
    if(type != PNG_COLOR_TYPE_RGB) {
        // TODO(morrita): Support RGBA and other formats.
        std::cerr << "Unsupported type:" << type << std::endl;
        abort();
    }

    int width = png_get_image_width(png, info);
    int height = png_get_image_height(png, info);
    RgbImage result(static_cast<size_t>(width), static_cast<size_t>(height));

    png_read_update_info(png, info);

    png_bytep* rows = (png_bytep*)malloc(sizeof(png_bytep) * height);
    for (auto y = 0; y < height; ++y) {
        rows[y] = result.row(size_t(y));
    }

    png_read_image(png, rows);

    png_free(png, rows);
    png_destroy_read_struct(&png, &info, &end_info);

    return result;
}

BayerImage read_phone_raw16(const std::string& filename) {
    return read_raw16(filename, 4032, 3032);
}

BayerImage read_raw16(const std::string& filename, size_t width, size_t height) {
    hv::Trace t("read_raw16");
    BayerImage image{width, height};
    std::ifstream file(filename);
    file.read(reinterpret_cast<char*>(image.data()), image.bytes());
    return image;
}


}