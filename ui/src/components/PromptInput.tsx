import { Box, Text, useInput } from 'ink';
import { useState } from 'react';

const ACCENT = '#7C3AED';
const DIM = '#6B7280';

interface PromptInputProps {
  value?: string;
  placeholder?: string;
  prompt?: string;
  onSubmit?: (value: string) => void;
  onChange?: (value: string) => void;
}

export function PromptInput({
  value: controlledValue,
  placeholder = '',
  prompt = '❯',
  onSubmit,
  onChange,
}: PromptInputProps) {
  const [internalValue, setInternalValue] = useState('');
  const value = controlledValue ?? internalValue;

  useInput((input, key) => {
    if (key.return && value.trim()) {
      onSubmit?.(value);
      if (controlledValue === undefined) setInternalValue('');
      return;
    }

    if (key.backspace || key.delete) {
      const next = value.slice(0, -1);
      if (controlledValue === undefined) setInternalValue(next);
      onChange?.(next);
      return;
    }

    const next = value + input;
    if (controlledValue === undefined) setInternalValue(next);
    onChange?.(next);
  });

  return (
    <Box flexDirection="column">
      <Box>
        <Text color={ACCENT}>{prompt} </Text>
        {value ? (
          <Text>{value}</Text>
        ) : (
          <Text color={DIM}>{placeholder}</Text>
        )}
        <Text color={ACCENT}>_</Text>
      </Box>
    </Box>
  );
}
