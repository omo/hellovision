load("@halide//:halide.bzl", "halide_library", "halide_language_copts", "halide_language_linkopts", "halide_runtime_linkopts")

cc_binary(
  name = "halide",
  srcs = ["main.cc"],
  deps = [
    ":mixedbag",
    ":hello",
    ":lesson02",
    ":lesson04",
    ":lesson07",
    ":lesson16",
    "//hello/helpers",
    "@com_github_gflags_gflags//:gflags",
    "@halide//:halide_buffer",
  ]
)

halide_library(
  name = "hello",
  srcs = ["hello.cc"],
  extra_outputs=["stmt", "schedule"],
)

halide_library(
  name = "lesson02",
  srcs = ["lesson02.cc"],
  debug_codegen_level = 0, # The default is zero.
  extra_outputs=["stmt", "html", "schedule"],
)

halide_library(
  name = "lesson07",
  srcs = ["lesson07.cc"],
  extra_outputs=["stmt", "html", "schedule"],
)

halide_library(
  name = "lesson16",
  srcs = ["lesson16.cc"],
  generator_args = "layout=interleaved",
  extra_outputs=["stmt", "html", "schedule"],
)

cc_library(
  name = "lesson04",
  srcs = ["lesson04.cc"],
  copts = halide_language_copts(),
  linkopts = halide_language_linkopts() + halide_runtime_linkopts(),
  deps = [
    ":mixedbag",
    "@halide//:language",
  ]
)

cc_library(
  name = "mixedbag",
  hdrs = ["mixedbag.h"],
  deps = [
    "@halide//:language",
  ]
)
