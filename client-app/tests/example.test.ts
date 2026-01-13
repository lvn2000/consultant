import { describe, it, expect } from 'vitest'

describe('Example Test', () => {
  it('should pass', () => {
    expect(1 + 1).toBe(2)
  })

  it('should work with async', async () => {
    const result = await Promise.resolve(42)
    expect(result).toBe(42)
  })
})
