
#include "halide/mixedbag.h"


#include "Halide.h"
#include <iostream>
#include <cstdio>

namespace h = Halide;

void run_lesson04() {
  std::cout << "Hello Lesson04!" << std::endl;

  h::Var x("x"), y("y");

  {
    h::Func gradient("gradient");
    gradient(x, y) = x + y;
    gradient.trace_stores();
    // h::Buffer<int> output = gradient.realize(8, 8);
  }

  {
    h::Func parallel_gradient("parallel_gradient");
    parallel_gradient(x, y) = x + y;
    parallel_gradient.trace_stores();
    parallel_gradient.parallel(y);
    // parallel_gradient.realize(8, 8);
  }

  {
    h::Func f("f");
    f(x, y) = sin(x) + print(cos(y), " x =", x, ", y =", y);
    // f.realize(8, 8);
  }

  {
    h::Func f("f");
    f(x, y) = sin(x) + print_when(x == 37 && y == 42, cos(y), "<- cos(y) on x = 37, y = 42");
    f.realize(128, 128);
  }

  {
    h::Func f("f");
    f(x, y) = sin(x) + print(cos(y), " x =", x, ", y =", y);
    std::cout << f.value() << std::endl;
  }
}

void run_lesson05() {
  h::Var x("x"), y("y");

  {
    h::Func gradient("gradient");
    gradient(x, y) = x + y;
    gradient.trace_stores();
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_col_major");
    gradient(x, y) = x + y;
    gradient.trace_stores();
    gradient.reorder(y, x);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_split");
    gradient(x, y) = x + y;
    gradient.trace_stores();

    h::Var x_outer("x_outer"), x_inner("x_inner");
    gradient.split(x, x_outer, x_inner, 2);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_fused");
    gradient(x, y) = x + y;
    gradient.trace_stores();

    h::Var fused("fused");
    gradient.fuse(x, y, fused);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_tiled");
    gradient(x, y) = x + y;
    gradient.trace_stores();

    h::Var x_outer("xo"), x_inner("xi"), y_outer("yo"), y_inner("yi");
    gradient.tile(x, y, x_outer, x_inner, y_outer, y_inner, 4, 4);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_in_vector");
    gradient(x, y) = x + y;
    h::Var x_outer("xo"), x_inner("xi");
    gradient.split(x, x_outer, x_inner, 4);
    gradient.vectorize(x_inner);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_in_vector2");
    gradient(x, y) = x + y;
    gradient.vectorize(x, 4);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_unroll");
    gradient(x, y) = x + y;
    gradient.unroll(x, 2);
    gradient.print_loop_nest();
    printf("\n");    
  }

  {
    h::Func gradient("gradient_fast");
    gradient(x, y) = x + y;
    h::Var xo("xo"), xi("xi"), yo("yo"), yi("yi"), ti("ti");

    gradient
      .tile(x, y, xo, yo, xi, yi, 64, 64)
      .fuse(xo, yo, ti)
      .parallel(ti);

    h::Var xio("xio"), yio("yio"), xii("xii"), yii("yii");
    gradient
      .tile(xi, yi, xio, yio, xii, yii, 4, 2)
      .vectorize(xii)
      .unroll(yii);

    gradient.print_loop_nest();
    h::Buffer<int> result = gradient.realize(350, 250);
    printf("\n");    

  }
}

void run_lesson09() {
  h::Var x("x"), y("y");

  {
    h::Func f;
    f(x, y) = (x + y)/100.0f;
    h::RDom r(0, 50);
    f(x, r) = f(x, r) * f(x, r);
    f.print_loop_nest();
    h::Buffer<float> result = f.realize(100, 100);
  }
}

// http://halide-lang.org/tutorials/tutorial_lesson_13_tuples.html
void run_lesson13() {
  h::Var x;
  h::Func input_func;
  input_func(x) = h::sin(x);
  h::Buffer<float> input = input_func.realize(100);

  h::Func arg_max;
  arg_max() = {0, input(0)};
  h::RDom r(1, 99);
  h::Expr oi = arg_max()[0];
  h::Expr ov = arg_max()[1];
  h::Expr ni = h::select(oi < input(r), r, oi);
  h::Expr nv = h::max(ov, input_func(r));
  arg_max() = {ni, nv};

  h::Realization result = arg_max.realize();
  // Needs explicit assignments to tell the buffer type.
  h::Buffer<int> r0 = result[0];
  h::Buffer<float> r1 = result[1];
  std::cout << "arg:" << r0(0) << std::endl;
  std::cout << "max:" << r1(0) << std::endl;
}