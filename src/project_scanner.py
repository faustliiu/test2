#!/usr/bin/env python3
"""
项目结构及内容提取脚本 (已添加自排除功能)
将指定目录的树形结构和文件内容保存到一个文本文件中。
"""

import os
import sys
import argparse
from pathlib import Path

# 【新增】获取脚本自身的绝对路径，用于后续排除自身
SCRIPT_PATH = Path(__file__).resolve()

def should_ignore(path, ignore_list, script_path):
    """检查路径是否在忽略列表中，或者是脚本自身"""
    # 【核心排除逻辑】如果是脚本文件自身，则跳过
    if path.resolve() == script_path:
        return True
    # 原有的忽略目录检查
    for ignore in ignore_list:
        if ignore in path.parts:
            return True
    return False

def read_file_content(file_path):
    """尝试以多种编码读取文件内容"""
    encodings = ['utf-8', 'gbk', 'latin-1', 'utf-16']
    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                return f.read()
        except (UnicodeDecodeError, PermissionError):
            continue
    return f"【错误：无法解码文件，可能为二进制文件】"

def generate_project_report(root_dir, output_file, ignore_dirs=None):
    """生成项目报告"""
    if ignore_dirs is None:
        ignore_dirs = ['.git', '__pycache__', 'node_modules', '.idea', '.vscode', 
                       'venv', 'env', 'dist', 'build', 'target', 'out', '.gradle', 'bin']
    
    root_path = Path(root_dir).resolve()
    output_path = Path(output_file)
    
    # 【新增】提示信息
    print(f"正在扫描目录: {root_path}")
    print(f"忽略目录: {', '.join(ignore_dirs)}")
    print(f"已排除脚本自身: {SCRIPT_PATH.name}")
    print("-" * 50)
    
    with open(output_path, 'w', encoding='utf-8') as report:
        # 写入标题和基本信息
        report.write(f"项目目录: {root_path}\n")
        report.write(f"生成时间: {os.path.basename(root_path)}\n")
        report.write(f"扫描脚本: {SCRIPT_PATH.name} (已自动排除)\n")
        report.write("=" * 60 + "\n\n")
        
        file_count = 0
        skip_count = 0
        
        # 遍历目录并处理文件
        for current_path, dirs, files in os.walk(root_path):
            current_path_obj = Path(current_path)
            
            # 移除需要忽略的目录（避免遍历）
            dirs[:] = [d for d in dirs if not should_ignore(current_path_obj / d, ignore_dirs, SCRIPT_PATH)]
            
            # 计算相对路径和缩进
            relative_path = current_path_obj.relative_to(root_path)
            indent_level = len(relative_path.parts)
            
            # 写入当前目录
            if current_path != str(root_path):
                report.write("  " * (indent_level - 1) + f"├── [{relative_path.name}]/\n")
            
            # 写入当前目录下的文件
            for i, file in enumerate(sorted(files)):
                file_path = current_path_obj / file
                
                # 【核心排除逻辑】跳过脚本自身和忽略列表中的文件
                if should_ignore(file_path, ignore_dirs, SCRIPT_PATH):
                    skip_count += 1
                    continue
                
                file_count += 1
                
                # 树形结构前缀
                is_last_file = (i == len(files) - 1) and (not dirs)
                prefix = "  " * indent_level + ("└── " if is_last_file else "├── ")
                report.write(f"{prefix}{file}\n")
                
                # 文件完整路径和内容
                report.write("  " * (indent_level + 1) + f"路径: {file_path}\n")
                
                # 读取并写入文件内容（如果是文本文件）
                text_extensions = ['.txt', '.py', '.java', '.js', '.c', '.cpp', '.h', 
                                  '.html', '.css', '.json', '.xml', '.md', '.yml', '.yaml',
                                  '.properties', '.gradle', '.kt', '.sql', '.sh', '.bat']
                if file_path.suffix.lower() in text_extensions:
                    content = read_file_content(file_path)
                    report.write("  " * (indent_level + 1) + "内容:\n")
                    report.write("  " * (indent_level + 2) + "-" * 40 + "\n")
                    
                    # 为内容添加缩进
                    for line in content.splitlines():
                        report.write("  " * (indent_level + 2) + line + "\n")
                    
                    report.write("  " * (indent_level + 2) + "-" * 40 + "\n")
                else:
                    report.write("  " * (indent_level + 1) + f"类型: 二进制或未支持文本格式（{file_path.suffix}）\n")
                
                report.write("\n")
        
        report.write("=" * 60 + "\n")
        report.write(f"扫描完成！\n")
        report.write(f"总计处理文件: {file_count} 个\n")
        report.write(f"跳过文件/目录: {skip_count} 个 (包括脚本自身和忽略列表)\n")
    
    print(f"报告已生成: {output_path}")
    print(f"文件大小: {output_path.stat().st_size / 1024:.2f} KB")
    print(f"处理文件数: {file_count}，跳过文件数: {skip_count}")
    return file_count

def main():
    parser = argparse.ArgumentParser(description="提取项目结构和文件内容 (自动排除脚本自身)")
    parser.add_argument("directory", nargs="?", default=".", help="要扫描的目录（默认为当前目录）")
    parser.add_argument("-o", "--output", default="project_report.txt", help="输出文件名")
    parser.add_argument("--no-ignore", action="store_true", help="不使用默认忽略规则")
    
    args = parser.parse_args()
    
    ignore_dirs = None if args.no_ignore else None  # 使用函数内的默认值
    
    try:
        generate_project_report(args.directory, args.output, ignore_dirs)
    except KeyboardInterrupt:
        print("\n用户中断操作。")
        sys.exit(1)
    except Exception as e:
        print(f"发生错误: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()