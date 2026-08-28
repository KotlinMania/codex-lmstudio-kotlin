#if canImport(Testing)
import Testing
import CodexLmstudio

@Suite("CodexLMStudio Swift Export Tests")
struct CodexLMStudioExportTests {
    @Test("CodexLmstudio swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "CodexLmstudio swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import CodexLmstudio

final class CodexLMStudioExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "CodexLmstudio swift module imported cleanly")
    }
}
#endif
