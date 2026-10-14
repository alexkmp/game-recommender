#!/bin/bash

# Waiting for Ollama server
until curl -s http://localhost:11434/api/tags > /dev/null; do
    echo "Waiting for Ollama to start..."
    sleep 2
done

echo "Ollama is running. Starting model download..."

# Load models
ollama pull qwen2.5:7b
ollama pull mxbai-embed-large

echo "All models have been loaded."