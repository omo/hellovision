
#include "Halide.h"

// Copied from 
// https://github.com/halide/Halide/tree/master/apps/bazeldemo
namespace {

class HelloHalide : public Halide::Generator<HelloHalide> {
 public:
  GeneratorParam<bool> vectorize{"vectorize", true};
  GeneratorParam<bool> parallelize{"parallelize", true};

  Input<Buffer<float>> input{"input", 2};
  Input<float> scale{"scale"};

  Output<Buffer<float>> output{"output", 2};

  void generate() {
    output(x, y) = input(x, y) * scale;
  }
  void schedule() {
    if (vectorize) {
      output.vectorize(x, natural_vector_size<float>());
    }
    if (parallelize) {
      output.parallel(y);
    }
  }

 private:
  Var x, y;
};

}  // namespace

HALIDE_REGISTER_GENERATOR(HelloHalide, hello)
