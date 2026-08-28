#if canImport(Testing)
import Testing
import CodexLMStudio

@Suite("CodexLMStudio Swift Export Tests")
struct CodexLMStudioExportTests {
    @Test("CodexLMStudio swift module imported cleanly")
    func testSwiftModuleLoads() throws {
        #expect(Bool(true), "CodexLMStudio swift module imported cleanly")
    }
}
#elseif canImport(XCTest)
import XCTest
import CodexLMStudio

final class CodexLMStudioExportTests: XCTestCase {
    func testSwiftModuleLoads() throws {
        XCTAssertTrue(true, "CodexLMStudio swift module imported cleanly")
    }
}
#endif
