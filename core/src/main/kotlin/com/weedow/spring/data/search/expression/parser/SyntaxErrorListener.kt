package com.weedow.spring.data.search.expression.parser

import org.antlr.v4.runtime.ANTLRErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/**
 * Interface to listen and manage the Parser syntax errors.
 */
interface SyntaxErrorListener : ANTLRErrorListener {

    /**
     * List of syntax errors detected by the Parser
     */
    val syntaxErrors: List<SyntaxError>
        get() = listOf()

    /**
     * This method is called by the Parser when a syntax error is detected.
     *
     * @param recognizer What parser got the error. From this object, you can access the context as well as the input stream.
     * @param offendingSymbol The offending token in the input token stream, unless recognizer is a lexer (then it's null). If no viable alternative error, e has token at which we started production for the decision.
     * @param line The line number in the input where the error occurred.
     * @param charPositionInLine The character position within that line where the error occurred.
     * @param msg The message to emit.
     * @param e The exception generated by the parser that led to the reporting of an error. It is null in the case where the parser was able to recover in line without exiting the surrounding rule.
     */
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException)

}