@echo off
copy /B /Y repositories\com.e1c.edt.ai.repository\target\com.e1c.edt.ai.repository-*.zip Z:\Transfer\ai_plugin\com.e1c.edt.ai.repository.zip
copy /B /Y repositories\com.e1c.edt.semantic.repository\target\com.e1c.edt.semantic.repository-*.zip Z:\Transfer\ai_plugin\com.e1c.edt.semantic.repository.zip
copy /B /Y repositories\com.e1c.edt.ui.eclipse.repository\target\com.e1c.edt.ui.eclipse.repository-*.zip Z:\Transfer\ai_plugin\com.e1c.edt.ui.eclipse.repository.zip
echo EDT AI repo: jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.ai.repository.zip/!
echo EDT semantic repo: jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.semantic.repository.zip/!
echo Eclipse AI repo: jar:file:Z:/Transfer/ai_plugin/com.e1c.edt.ui.eclipse.repository.zip/!