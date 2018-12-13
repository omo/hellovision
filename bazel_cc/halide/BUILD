load("@halide//:halide.bzl", "halide_library")

cc_binary(
  name = "halide",
  srcs = ["main.cc"],
  deps = [
    ":hello",
    "@halide//:halide_buffer",
  ]
)

halide_library(
  name = "hello",
  srcs = ["hello.cc"],
  extra_outputs=["stmt", "schedule"],
)