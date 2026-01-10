// swift-tools-version:5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "RenamedExamples",
    platforms: [
        .macOS(.v12)
    ],
    dependencies: [
        .package(path: "../../sdks/swift")
    ],
    targets: [
        .executableTarget(
            name: "BasicUsage",
            dependencies: [
                .product(name: "Renamed", package: "swift")
            ],
            path: ".",
            sources: ["BasicUsage.swift"]
        )
    ]
)
