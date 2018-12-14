load("@halide//:halide.bzl", "halide_library")

cc_binary(
  name = "halide",
  srcs = ["main.cc"],
  deps = [
    ":hello",
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