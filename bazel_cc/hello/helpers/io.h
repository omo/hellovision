#ifndef HELLO_HELPERS_IO_H
#define HELLO_HELPERS_IO_H

#include <string>
#include "hello/helpers/image.h"

namespace hv {

void write_png(const std::string& filename, const Image<uint8_t, 3>& image);

} // namespace hv

#endif // HELLO_HELPERS_IO_H