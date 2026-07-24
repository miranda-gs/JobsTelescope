import { Box } from 'ink';

interface CenterProps {
  children: React.ReactNode;
}

export function Center({ children }: CenterProps) {
  return (
    <Box
      flexDirection="column"
      justifyContent="center"
      alignItems="center"
      height="100%"
    >
      {children}
    </Box>
  );
}
