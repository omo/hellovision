package(default_visibility = ["//visibility:public"])

cc_binary(
    name = "hello",
    srcs = ["helloworld.cc"],
    deps = [
        "//hello/helpers",
        "@com_google_absl//absl/strings",
        "@com_github_gflags_gflags//:gflags",
        "@jsoncpp//:jsoncpp",
    ]
)

cc_test(
    name = "hello_test",
    srcs = [ "hello_test.cc" ],
    deps = [
         "@com_google_googletest//:gtest_main",
    ],
)