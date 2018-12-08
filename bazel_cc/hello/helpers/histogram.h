#ifndef HELPERS_HISTOGRAM_H
#define HELPERS_HISTOGRAM_H

#include <algorithm>
#include "hello/helpers/image.h"

namespace hv {

typedef std::vector<size_t> Histogram;

template<typename T>
Histogram histogram_of(const Image<T, 1>& image) {
    Histogram result(1 << (sizeof(T) * 8), 0u);
    auto& buf = image.buffer();
    std::for_each(buf.begin(), buf.end(), [&result](auto p) { result[p]++; });
    return result;
}

}

#endif // HELPERS_HISTOGRAM_H