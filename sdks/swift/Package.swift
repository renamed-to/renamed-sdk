// swift-tools-version:5.9
// The swift-tools-version declares the minimum version of Swift required to build this package.

import PackageDescription

let package = Package(
    name: "Renamed",
    platforms: [
        .iOS(.v15),
        .macOS(.v12)
    ],
    products: [
        .library(
            name: "Renamed",
            targets: ["Renamed"]
        )
    ],
    targets: [
        .target(
            name: "Renamed",
            dependencies: [],
            path: "Sources/Renamed"
        ),
        .testTarget(
            name: "RenamedTests",
            dependencies: ["Renamed"],
            path: "Tests/RenamedTests"
        )
    ]
)
