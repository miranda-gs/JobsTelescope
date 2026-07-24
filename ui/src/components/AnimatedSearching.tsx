import { Text } from 'ink';
import { useEffect, useState } from 'react';

const SPINNER = ['⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'];

export function AnimatedSearching() {
  const [frame, setFrame] = useState(0);

  useEffect(() => {
    const interval = setInterval(() => {
      setFrame((prev) => (prev + 1) % SPINNER.length);
    }, 100);
    return () => clearInterval(interval);
  }, []);

  return <Text dimColor>Procurando {SPINNER[frame]}</Text>;
}
