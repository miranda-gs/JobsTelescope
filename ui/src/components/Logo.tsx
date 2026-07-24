import { Box, Text } from 'ink';
import { LOGO } from '../lib/logo.ts';

const ACCENT = '#7C3AED';

export function Logo() {
  return (
    <Box flexDirection="column" alignItems="center">
      {LOGO.split('\n').map((line, i) => (
        <Text key={i} color={ACCENT}>{line}</Text>
      ))}
    </Box>
  );
}
