#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""修复 ObjectSizeCalculator 导入问题"""

import os
import re

files_to_fix = [
    "Consistency-Aware-Authenticated-Learned-Index/src/index/PVLB_tree_index/PVLBTree.java",
    "Consistency-Aware-Authenticated-Learned-Index/src/index/PVLB_tree_index/PVLBTreeChain.java",
    "Consistency-Aware-Authenticated-Learned-Index/src/index/HPVL_tree_index/HPVLIndex.java",
]

for filepath in files_to_fix:
    if not os.path.exists(filepath):
        print(f"文件不存在: {filepath}")
        continue
    
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 注释掉 import 语句
    content = content.replace(
        'import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;',
        '// import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator; // Java 9+ 不可用'
    )
    
    # 注释掉 ObjectSizeCalculator 的使用
    content = re.sub(
        r'(\s+)long (piSize|sz) = ObjectSizeCalculator\.getObjectSize\([^)]+\);',
        r'\1// long \2 = ObjectSizeCalculator.getObjectSize(...); // Java 9+ 不可用',
        content
    )
    
    # 注释掉相关的 println
    content = re.sub(
        r'(\s+)System\.out\.println\(".*?size:" \+ [^;]+\);',
        r'\1// System.out.println("...size:..."); // Java 9+ 不可用\n\1System.out.println("Index size: (需要 Java 8)");',
        content
    )
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)
    
    print(f"已修复: {filepath}")

print("\n所有文件修复完成!")


