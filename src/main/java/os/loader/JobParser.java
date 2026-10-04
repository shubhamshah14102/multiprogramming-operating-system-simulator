package os.loader;

import os.machine.Instruction;
import os.machine.Opcode;
import os.machine.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Strict parser for deterministic text jobs.
 *
 * <pre>
 * JOB 7 PRIORITY 2
 * LOADI R0 10
 * LOADI R1 3
 * SUB R0 R1
 * SVC 1
 * HALT
 * END
 * </pre>
 */
public final class JobParser {
    public List<JobDefinition> parse(String source) {
        if (source == null) {
            throw new IllegalArgumentException("Job source cannot be null");
        }

        List<JobDefinition> jobs = new ArrayList<>();
        Integer processId = null;
        int priority = 0;
        List<Integer> words = new ArrayList<>();

        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = stripComment(lines[index]).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] tokens = line.split("[\\s,]+");
            String keyword = tokens[0].toUpperCase(Locale.ROOT);
            if ("JOB".equals(keyword)) {
                if (processId != null) {
                    throw error(lineNumber, "Nested JOB declaration");
                }
                requireCount(tokens, 4, lineNumber);
                if (!"PRIORITY".equalsIgnoreCase(tokens[2])) {
                    throw error(lineNumber, "Expected PRIORITY in JOB header");
                }
                processId = parseNumber(tokens[1], lineNumber);
                priority = parseNumber(tokens[3], lineNumber);
            } else if ("END".equals(keyword)) {
                requireCount(tokens, 1, lineNumber);
                if (processId == null) {
                    throw error(lineNumber, "END without JOB");
                }
                if (words.isEmpty()) {
                    throw error(lineNumber, "Job contains no instructions");
                }
                jobs.add(new JobDefinition(
                        processId,
                        priority,
                        new Program(words.stream().mapToInt(Integer::intValue).toArray())));
                processId = null;
                priority = 0;
                words.clear();
            } else {
                if (processId == null) {
                    throw error(lineNumber, "Instruction outside a JOB");
                }
                words.add(parseInstruction(tokens, lineNumber));
            }
        }

        if (processId != null) {
            throw error(lines.length, "Missing END for JOB " + processId);
        }
        return List.copyOf(jobs);
    }

    private int parseInstruction(String[] tokens, int lineNumber) {
        String mnemonic = tokens[0].toUpperCase(Locale.ROOT);
        return switch (mnemonic) {
            case "NOP" -> {
                requireCount(tokens, 1, lineNumber);
                yield Instruction.noOperands(Opcode.NOP);
            }
            case "HALT" -> {
                requireCount(tokens, 1, lineNumber);
                yield Instruction.noOperands(Opcode.HALT);
            }
            case "LOADI" -> {
                requireCount(tokens, 3, lineNumber);
                yield Instruction.registerImmediate(
                        Opcode.LOAD_IMMEDIATE,
                        parseRegister(tokens[1], lineNumber),
                        parseNumber(tokens[2], lineNumber));
            }
            case "LOAD" -> {
                requireCount(tokens, 3, lineNumber);
                yield Instruction.registerAddress(
                        Opcode.LOAD,
                        parseRegister(tokens[1], lineNumber),
                        parseNumber(tokens[2], lineNumber));
            }
            case "STORE" -> {
                requireCount(tokens, 3, lineNumber);
                yield Instruction.registerAddress(
                        Opcode.STORE,
                        parseRegister(tokens[1], lineNumber),
                        parseNumber(tokens[2], lineNumber));
            }
            case "MOVE", "ADD", "SUB" -> {
                requireCount(tokens, 3, lineNumber);
                Opcode opcode = switch (mnemonic) {
                    case "MOVE" -> Opcode.MOVE;
                    case "ADD" -> Opcode.ADD;
                    default -> Opcode.SUBTRACT;
                };
                yield Instruction.registers(
                        opcode,
                        parseRegister(tokens[1], lineNumber),
                        parseRegister(tokens[2], lineNumber));
            }
            case "JUMP" -> {
                requireCount(tokens, 2, lineNumber);
                yield Instruction.address(
                        Opcode.JUMP, parseNumber(tokens[1], lineNumber));
            }
            case "JZ" -> {
                requireCount(tokens, 3, lineNumber);
                yield Instruction.registerAndAddress(
                        Opcode.JUMP_IF_ZERO,
                        parseRegister(tokens[1], lineNumber),
                        parseNumber(tokens[2], lineNumber));
            }
            case "SVC" -> {
                requireCount(tokens, 2, lineNumber);
                yield Instruction.address(
                        Opcode.SVC, parseNumber(tokens[1], lineNumber));
            }
            case "SET_TIMER" -> {
                requireCount(tokens, 2, lineNumber);
                yield Instruction.address(
                        Opcode.SET_TIMER, parseNumber(tokens[1], lineNumber));
            }
            default -> throw error(lineNumber, "Unknown instruction: " + tokens[0]);
        };
    }

    private static int parseRegister(String token, int lineNumber) {
        if (!token.matches("(?i)R[0-7]")) {
            throw error(lineNumber, "Expected register R0 through R7: " + token);
        }
        return token.charAt(1) - '0';
    }

    private static int parseNumber(String token, int lineNumber) {
        try {
            return Integer.decode(token);
        } catch (NumberFormatException exception) {
            throw error(lineNumber, "Invalid integer: " + token);
        }
    }

    private static void requireCount(String[] tokens, int expected, int lineNumber) {
        if (tokens.length != expected) {
            throw error(
                    lineNumber,
                    "Expected " + (expected - 1) + " operand(s), found "
                            + (tokens.length - 1));
        }
    }

    private static String stripComment(String line) {
        int commentStart = line.indexOf('#');
        return commentStart < 0 ? line : line.substring(0, commentStart);
    }

    private static IllegalArgumentException error(int lineNumber, String message) {
        return new IllegalArgumentException("Line " + lineNumber + ": " + message);
    }
}
