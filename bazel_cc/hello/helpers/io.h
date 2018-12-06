#ifndef HELLO_HELPERS_IO_H
#define HELLO_HELPERS_IO_H

#include <string>
#include "hello/helpers/image.h"

namespace hv {

void write_png(const std::string& filename, const Image<uint8_t, 3>& image);

RawImage read_phone_raw16(const std::string& filename);
RawImage read_raw16(const std::string& filename, size_t width, size_t height);

} // namespace hv

#endif // HELLO_HELPERS_IO_H