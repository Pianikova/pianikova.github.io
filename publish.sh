#!/usr/bin/env bash
set -e
TARGET=/z/Transfer/ai_plugin
cp -f repositories/com.e1c.edt.ai.repository/target/com.e1c.edt.ai.repository-*.zip      "$TARGET/com.e1c.edt.ai.repository.zip"
cp -f repositories/com.e1c.edt.semantic.repository/target/com.e1c.edt.semantic.repository-*.zip "$TARGET/com.e1c.edt.semantic.repository.zip"
cp -f repositories/com.e1c.edt.ui.eclipse.repository/target/com.e1c.edt.ui.eclipse.repository-*.zip "$TARGET/com.e1c.edt.ui.eclipse.repository.zip"
echo
echo "[publish] Done. Copied 3 repository zips to Z:\\Transfer\\ai_plugin\\. Use as Eclipse update sites:"
echo "  EDT AI repo:       jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.ai.repository.zip!/"
echo "  EDT semantic repo: jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.semantic.repository.zip!/"
echo "  Eclipse AI repo:   jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.ui.eclipse.repository.zip!/"
