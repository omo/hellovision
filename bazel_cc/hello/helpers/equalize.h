#ifndef HELPERS_EQUALIZE_H
#define HELPERS_EQUALIZE_H

#include <algorithm>
#include "hello/helpers/image.h"

namespace hv {

RgbImage equalize_to_rgb(const RawImage& src);

}

#endif // HELPERS_EQUALIZE_H