const BLANK_INPUT_PATTERN = /^\s*$/

export function isBlankInput(value: string): boolean {
  return BLANK_INPUT_PATTERN.test(value)
}
