package(default_visibility = ["//visibility:public"])

cc_binary(
    name = "hello",
    srcs = ["helloworld.cc"],
    deps = [
        "@com_google_absl//absl/strings",
        "@jsoncpp//:jsoncpp",
        "@png_archive//:png"
    ]
)

cc_test(
    name = "hello_test",
    srcs = [ "hello_test.cc" ],
    deps = [
         "@com_google_googletest//:gtest_main",
    ],
)