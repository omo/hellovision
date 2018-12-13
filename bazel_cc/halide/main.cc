
#include "halide/hello.h"
#include "HalideBuffer.h"
#include <iostream>

int main(int argc, char** argv) {
  Halide::Runtime::Buffer<float> input(900, 900);
  Halide::Runtime::Buffer<float> output(900, 900);
  hello(input, 1.0f, output);
  std::cout << "Hello Halide (prematurely)!" << std::endl;
  return 0;
}