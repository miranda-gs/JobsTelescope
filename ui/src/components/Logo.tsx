import { Box, Text } from 'ink';
import { LOGO } from '../lib/logo.ts';
import { ACCENT } from '../lib/constants.ts';

export function Logo() {
  return (
    <Box flexDirection="column" alignItems="center">
      {LOGO.split('\n').map((line, i) => (
        <Text key={i} color={ACCENT}>{line}</Text>
      ))}
      <Text> </Text>
    </Box>
  );
}
